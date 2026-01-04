import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FloydWarshallVisualizer extends JFrame {

    // ======= CONFIG =======
    static final int INF = 1_000_000_000;

    // Demo: grafo con 6 nodos (0..5). Puedes cambiarlo.
    // Usa INF si no hay arista directa.
    private final int[][] graph = {
            {0,   3,   INF, 7,   INF, INF, 2},
            {3,   0,   2,   INF, 3,   INF, 5},
            {INF, 2,   0,   1,   INF, 6, 2},
            {7,   INF, 1,   0,   2,   3, INF},
            {INF, 3,   INF, 2,   0,   2, INF},
            {INF, INF, 6,   3,   2,   0, INF},
            {5, 3,   INF, 2,   INF,   2, 6}
    };

    // Coordenadas de cada nodo (para dibujar bonito).
    private final Point[] pos = {
            new Point(120, 120),
            new Point(320,  90),
            new Point(520, 160),
            new Point(520, 360),
            new Point(300, 430),
            new Point(120, 320),
            new Point(120, 240)

    };

    // ======= ESTADO DEL VISUALIZADOR =======
    private int n;
    private int[][] dist;
    private int[][] next; // para reconstruir camino
    private int currentK = -1;
    private boolean running = false;

    private int startNode = 0;
    private int endNode = 5;

    private final GraphPanel panel = new GraphPanel();
    private final JLabel status = new JLabel("Listo.");
    private final JComboBox<Integer> startBox = new JComboBox<>();
    private final JComboBox<Integer> endBox = new JComboBox<>();
    private final JButton runBtn = new JButton("Run / Animar");
    private final JButton resetBtn = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 200, 60); // ms por paso
    private javax.swing.Timer timer;

    // Algoritmo paso-a-paso: (k,i,j)
    private int k = 0, i = 0, j = 0;

    // Para mostrar camino final
    private List<Integer> finalPath = Collections.emptyList();

    public FloydWarshallVisualizer() {
        super("Floyd-Warshall (Swing Visual)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 560);
        setLocationRelativeTo(null);

        n = graph.length;

        for (int idx = 0; idx < n; idx++) {
            startBox.addItem(idx);
            endBox.addItem(idx);
        }
        startBox.setSelectedItem(startNode);
        endBox.setSelectedItem(endNode);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Start:"));
        top.add(startBox);
        top.add(new JLabel("End:"));
        top.add(endBox);
        top.add(new JLabel("Velocidad (ms/paso):"));
        speedSlider.setPreferredSize(new Dimension(160, 20));
        top.add(speedSlider);
        top.add(runBtn);
        top.add(resetBtn);

        add(top, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // Inicializa matrices
        reset();

        runBtn.addActionListener(e -> {
            if (!running) startAnimation();
            else stopAnimation("Pausado.");
        });

        resetBtn.addActionListener(e -> reset());

        startBox.addActionListener(e -> {
            startNode = (Integer) startBox.getSelectedItem();
            finalPath = Collections.emptyList();
            panel.repaint();
        });
        endBox.addActionListener(e -> {
            endNode = (Integer) endBox.getSelectedItem();
            finalPath = Collections.emptyList();
            panel.repaint();
        });

        // Click en nodos para elegir start/end rápido:
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int node = panel.pickNode(e.getPoint());
                if (node != -1) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        startNode = node;
                        startBox.setSelectedItem(node);
                        status.setText("Start = " + node);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        endNode = node;
                        endBox.setSelectedItem(node);
                        status.setText("End = " + node);
                    }
                    finalPath = Collections.emptyList();
                    panel.repaint();
                }
            }
        });
    }

    private void reset() {
        stopAnimation("Reseteado.");
        dist = new int[n][n];
        next = new int[n][n];
        finalPath = Collections.emptyList();

        // init dist + next
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                dist[a][b] = graph[a][b];
                if (a == b || graph[a][b] >= INF) next[a][b] = -1;
                else next[a][b] = b;
            }
        }

        currentK = -1;
        k = 0; i = 0; j = 0;
        panel.repaint();
    }

    private void startAnimation() {
        running = true;
        runBtn.setText("Pause");
        status.setText("Animando Floyd-Warshall...");

        int delay = Math.max(1, speedSlider.getValue());
        timer = new javax.swing.Timer(delay, e -> step());
        timer.start();
    }

    private void stopAnimation(String msg) {
        running = false;
        runBtn.setText("Run / Animar");
        if (timer != null) timer.stop();
        status.setText(msg);
        panel.repaint();
    }

    private void step() {
        // Ajusta velocidad en caliente
        if (timer != null) timer.setDelay(Math.max(1, speedSlider.getValue()));

        if (k >= n) {
            // terminó: reconstruye camino final
            currentK = -1;
            finalPath = reconstructPath(startNode, endNode);
            if (finalPath.isEmpty()) {
                status.setText("Terminado. No hay ruta de " + startNode + " a " + endNode);
            } else {
                int d = dist[startNode][endNode];
                status.setText("Terminado. Dist(" + startNode + "→" + endNode + ") = " + d
                        + " | Ruta: " + finalPath);
            }
            stopAnimation(status.getText());
            return;
        }

        currentK = k;

        // Paso Floyd: intenta mejorar dist[i][j] usando k como intermedio
        if (dist[i][k] < INF && dist[k][j] < INF) {
            int nd = dist[i][k] + dist[k][j];
            if (nd < dist[i][j]) {
                dist[i][j] = nd;
                next[i][j] = next[i][k]; // clave para reconstrucción
            }
        }

        // avanza (i,j,k)
        j++;
        if (j >= n) { j = 0; i++; }
        if (i >= n) { i = 0; k++; }

        // Actualiza status + repinta
        status.setText("k=" + currentK + " | probando mejora dist[" + i + "][" + j + "] con intermedio " + currentK);
        panel.repaint();
    }

    private List<Integer> reconstructPath(int u, int v) {
        if (u < 0 || v < 0 || u >= n || v >= n) return Collections.emptyList();
        if (u == v) return List.of(u);
        if (next[u][v] == -1) return Collections.emptyList();

        ArrayList<Integer> path = new ArrayList<>();
        path.add(u);

        int stepsGuard = 0;
        while (u != v) {
            u = next[u][v];
            if (u == -1) return Collections.emptyList();
            path.add(u);

            // guardia anti-bucles por si editas algo raro
            if (++stepsGuard > n + 5) return Collections.emptyList();
        }
        return path;
    }

    // ======= PANEL DE DIBUJO =======
    private class GraphPanel extends JPanel {

        GraphPanel() { setBackground(Color.WHITE); }

        int pickNode(Point p) {
            int r = 18;
            for (int idx = 0; idx < n; idx++) {
                Point c = pos[idx];
                if (p.distance(c) <= r) return idx;
            }
            return -1;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1) Dibuja aristas (undirected si hay peso en cualquiera de los sentidos)
            for (int a = 0; a < n; a++) {
                for (int b = a + 1; b < n; b++) {
                    int w = Math.min(graph[a][b], graph[b][a]);
                    if (w >= INF) continue;

                    // ¿Esta arista está en el camino final?
                    boolean inFinalPath = edgeInPath(a, b, finalPath);

                    g2.setStroke(new BasicStroke(inFinalPath ? 5f : 2f));
                    g2.setColor(inFinalPath ? new Color(30, 144, 255) : new Color(180, 180, 180));

                    Point p1 = pos[a], p2 = pos[b];
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                    // etiqueta del peso
                    int mx = (p1.x + p2.x) / 2;
                    int my = (p1.y + p2.y) / 2;
                    g2.setColor(Color.BLACK);
                    g2.drawString(String.valueOf(w), mx + 4, my - 4);
                }
            }

            // 2) Dibuja nodos
            for (int idx = 0; idx < n; idx++) {
                Point c = pos[idx];
                int r = 18;

                boolean isStart = idx == startNode;
                boolean isEnd = idx == endNode;
                boolean isK = idx == currentK && running;

                // Color de relleno
                if (isStart) g2.setColor(new Color(0, 200, 90));
                else if (isEnd) g2.setColor(new Color(255, 90, 90));
                else if (isK) g2.setColor(new Color(255, 200, 0));
                else g2.setColor(new Color(240, 240, 240));

                g2.fillOval(c.x - r, c.y - r, 2 * r, 2 * r);
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(c.x - r, c.y - r, 2 * r, 2 * r);

                // label nodo
                g2.setColor(Color.BLACK);
                g2.drawString("N" + idx, c.x - 10, c.y + 5);
            }

            // 3) Info esquina
            g2.setColor(Color.GRAY);
            g2.drawString("Click IZQ = Start | Click DER = End", 10, 18);

            g2.dispose();
        }

        private boolean edgeInPath(int a, int b, List<Integer> path) {
            if (path == null || path.size() < 2) return false;
            for (int t = 0; t < path.size() - 1; t++) {
                int u = path.get(t), v = path.get(t + 1);
                if ((u == a && v == b) || (u == b && v == a)) return true;
            }
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FloydWarshallVisualizer().setVisible(true));
    }
}
