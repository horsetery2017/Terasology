package org.terasology.rendering.gui.menus;

import com.sun.corba.se.pept.transport.ListenerThread;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.procedure.TObjectDoubleProcedure;
import gnu.trove.procedure.TObjectIntProcedure;
import org.lwjgl.input.Keyboard;
import org.terasology.game.Terasology;
import org.terasology.logic.entities.StaticEntity;
import org.terasology.performanceMonitor.PerformanceMonitor;
import org.terasology.rendering.gui.components.UIText;
import org.terasology.rendering.gui.framework.UIDisplayContainer;
import org.terasology.rendering.gui.framework.UIDisplayRenderer;

import javax.vecmath.Vector2f;
import java.util.*;

/**
 * UI element that graphs performance metrics
 * @author Immortius <immortius@gmail.com>
 */
public class UIMetrics extends UIDisplayRenderer {

    private static final int METRIC_LINES = 10;

    private Mode _currentMode = Mode.Off;
    
    /* DISPLAY ELEMENTS */
    private final UIText _headerLine;
    private final List<UIText> _metricLines;

    /**
     * Init. the HUD.
     */
    public UIMetrics() {
        setOverlay(true);
        _headerLine = new UIText(new Vector2f(4, 70));
        addDisplayElement(_headerLine);
        _metricLines = new ArrayList<UIText>();
        for (int i = 0; i < METRIC_LINES; ++i)
        {
            UIText line = new UIText(new Vector2f(4, 86 + 16 * i));
            _metricLines.add(line);
            addDisplayElement(line);
        }

        update();
    }


    /**
     * Renders the HUD on the screen.
     */
    @Override
    public void render() {
        super.render();
    }

    @Override
    public void update() {
        super.update();

        _headerLine.setVisible(_currentMode.visible);
        _headerLine.setText(_currentMode.displayText);
        _currentMode.updateLines(_metricLines);
    }

    @Override
    public void processKeyboardInput(int key) {
        super.processKeyboardInput(key);

        if (!isVisible())
            return;

        if (key == Keyboard.KEY_F4) {
            _currentMode = Mode.nextMode(_currentMode);
        }
    }

    private enum Mode
    {
        Off("", false)
                {
                    @Override
                    public void updateLines(List<UIText> lines) {
                        for (UIText line : lines)
                        {
                            line.setVisible(false);
                        }
                    }
                },

        RunningMean("Running Means", true)
                {
                    @Override
                    public void updateLines(List<UIText> lines) {
                        displayMetrics(PerformanceMonitor.getRunningMean(), lines);
                    }
                },
        DecayingSpikes("Spikes", true)
                {
                    @Override
                    public void updateLines(List<UIText> lines) {
                        displayMetrics(PerformanceMonitor.getDecayingSpikes(), lines);
                    }
                },
        RunningThreads("Running Threads", true)
                {
                    @Override
                    public void updateLines(List<UIText> lines) {
                        final SortedSet<String> threads = new TreeSet<String>();
                        PerformanceMonitor.getRunningThreads().forEachEntry(new TObjectIntProcedure<String>() {
                            public boolean execute(String s, int i) {
                                threads.add(String.format("%s (%d)", s, i));
                                return true;
                            }
                        });
                        int line = 0;
                        for (String thread : threads)
                        {
                            lines.get(line).setVisible(true);
                            lines.get(line).setText(thread);
                            line++;
                            if (line >= lines.size()) break;
                        }
                        for (;line < lines.size(); line++)
                        {
                            lines.get(line).setVisible(false);
                        }
                    }
                };

        public final String displayText;
        public final boolean visible;

        private Mode(String display, boolean visible)
        {
            this.displayText = display;
            this.visible = visible;
        }

        public abstract void updateLines(List<UIText> lines);

        public static Mode nextMode(Mode current)
        {
            switch (current)
            {
                case Off:
                    return RunningMean;
                case RunningMean:
                    return DecayingSpikes;
                case DecayingSpikes:
                    return RunningThreads;
                default:
                    return Off;
            }
        }

        private static void displayMetrics(TObjectDoubleMap<String> metrics, List<UIText> lines)
        {
            final List<String> activities = new ArrayList<String>();
            final List<Double> values = new ArrayList<Double>();
            sortMetrics(metrics, activities, values);

            for (int i = 0; i < lines.size() && i < activities.size(); ++i)
            {
                UIText line = lines.get(i);
                line.setVisible(true);
                line.setText(String.format("%s: %.2fms", activities.get(i), values.get(i)));
            }
            for (int i = activities.size(); i < lines.size(); ++i)
            {
                lines.get(i).setVisible(false);
            }
        }

        private static void sortMetrics(TObjectDoubleMap<String> metrics, final List<String> activities, final List<Double> values) {
            metrics.forEachEntry(new TObjectDoubleProcedure<String>() {
                public boolean execute(String s, double v) {
                    boolean inserted = false;
                    for (int i = 0; i < values.size(); i++)
                    {
                        if (v > values.get(i))
                        {
                            values.add(i, v);
                            activities.add(i, s);
                            inserted = true;
                            break;
                        }
                    }
                    if (!inserted)
                    {
                        activities.add(s);
                        values.add(v);
                    }
                    return true;
                }
            });
        }
    }

}
