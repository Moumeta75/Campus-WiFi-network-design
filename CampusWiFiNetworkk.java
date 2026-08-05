package campuswifinetworkk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CampusWiFiNetworkk extends JFrame {

    // Data
    private final LinkedHashMap<String, Point> buildings = new LinkedHashMap<>();
    private final List<Conn> connections = new ArrayList<>();
    private List<Conn> mst = new ArrayList<>();

    // UI
    private final GraphPanel graphPanel = new GraphPanel();
    private final JLabel costLabel = new JLabel("Total MST Cost: 0.00");
    private final JLabel sourceLabel = new JLabel("Source: None");
    private String sourceBuilding = null;

    // Background map
    private final Image campusMapImg = new ImageIcon("C:\\Users\\moumi\\OneDrive\\Pictures\\Screenshot 2025-11-29 222621.png").getImage();

    // ⭐ NEW: Building icon
    private final Image buildingIcon = new ImageIcon("C:\\Users\\moumi\\OneDrive\\Pictures\\vector-isometric-business-center-building-260nw-364246337.png").getImage();

    public CampusWiFiNetworkk() {
        super("CampusWiFiNetwork");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("BUP WiFi Network Design", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        top.add(title, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT));
        info.add(costLabel);
        info.add(Box.createHorizontalStrut(20));
        info.add(sourceLabel);
        top.add(info, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(graphPanel, BorderLayout.CENTER);

        // Bottom controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addB = new JButton("Add Buildings (loop)");
        JButton delB = new JButton("Delete Building");
        JButton addC = new JButton("Add Connections (loop)");
        JButton editC = new JButton("Edit Connection");
        JButton delC = new JButton("Delete Connection");
        JButton changeSource = new JButton("Change Source");
        JButton resetDefault = new JButton("Reset Default Buildings");
        JButton saveBtn = new JButton("Save (simple text)");
        JButton loadBtn = new JButton("Load (simple text)");

        controls.add(addB);
        controls.add(delB);
        controls.add(addC);
        controls.add(editC);
        controls.add(delC);
        controls.add(changeSource);
        controls.add(resetDefault);
        controls.add(saveBtn);
        controls.add(loadBtn);

        add(controls, BorderLayout.SOUTH);

        // Button actions
        addB.addActionListener(e -> addBuildingsLoop());
        delB.addActionListener(e -> deleteBuildingDialog());
        addC.addActionListener(e -> addConnectionsLoop());
        editC.addActionListener(e -> editConnectionDialog());
        delC.addActionListener(e -> deleteConnectionDialog());
        changeSource.addActionListener(e -> changeSourceDialog());
        resetDefault.addActionListener(e -> {
            loadDefaultBuildings();
            computeMSTAndRefresh();
        });
        saveBtn.addActionListener(e -> saveToTextFile());
        loadBtn.addActionListener(e -> loadFromTextFile());

        loadDefaultBuildings();
        computeMSTAndRefresh();
    }

    private static class Conn {
        String a, b;
        double w;

        Conn(String a, String b, double w) {
            this.a = a;
            this.b = b;
            this.w = w;
        }
    }

    private void loadDefaultBuildings() {
        buildings.clear();
        connections.clear();

        buildings.put("FST", new Point(200, 160));
        buildings.put("FBS", new Point(820, 160));
        buildings.put("FASS", new Point(160, 360));
        buildings.put("FSSS", new Point(860, 360));
        buildings.put("Academic", new Point(540, 300));
        buildings.put("Library", new Point(480, 520));
        buildings.put("Annex", new Point(820, 520));

        sourceBuilding = "Academic";
    }

    private void addBuildingsLoop() {
        while (true) {
            String name = JOptionPane.showInputDialog(this, "Enter building name (type 'done' to finish):");
            if (name == null) return;
            name = name.trim();
            if (name.equalsIgnoreCase("done")) break;
            if (name.isEmpty()) continue;

            if (buildings.containsKey(name)) {
                JOptionPane.showMessageDialog(this, "Building already exists.");
                continue;
            }

            int centerX = graphPanel.getWidth() / 2;
            int centerY = graphPanel.getHeight() / 2;
            int radius = 220;

            int n = buildings.size();
            double angle = (n * 2 * Math.PI / Math.max(6, n + 1));
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));

            buildings.put(name, new Point(x, y));
        }
        computeMSTAndRefresh();
    }

    private void deleteBuildingDialog() {
        if (buildings.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No buildings to delete.");
            return;
        }
        String[] names = buildings.keySet().toArray(new String[0]);
        String chosen = (String) JOptionPane.showInputDialog(this, "Select building to delete:",
                "Delete Building", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null) return;

        buildings.remove(chosen);
        connections.removeIf(c -> c.a.equals(chosen) || c.b.equals(chosen));

        computeMSTAndRefresh();
    }

    private void addConnectionsLoop() {
        if (buildings.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 buildings.");
            return;
        }
        while (true) {
            String[] names = buildings.keySet().toArray(new String[0]);

            String a = (String) JOptionPane.showInputDialog(this, "Select A:",
                    "Add Connection", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
            if (a == null) break;
            String b = (String) JOptionPane.showInputDialog(this, "Select B:",
                    "Add Connection", JOptionPane.PLAIN_MESSAGE, null, names, names[1]);
            if (b == null) break;
            if (a.equals(b)) continue;

            String wStr = JOptionPane.showInputDialog(this, "Enter weight:");
            if (wStr == null) break;

            try {
                double w = Double.parseDouble(wStr);
                connections.add(new Conn(a, b, w));
                computeMSTAndRefresh();
            } catch (Exception ignored) {
            }
        }
    }

    private void editConnectionDialog() {
        if (connections.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No connections.");
            return;
        }
        String[] conNames = connections.stream()
                .map(c -> c.a + " - " + c.b + " : " + c.w)
                .toArray(String[]::new);

        String sel = (String) JOptionPane.showInputDialog(this, "Select:",
                "Edit Connection", JOptionPane.PLAIN_MESSAGE, null, conNames, conNames[0]);
        if (sel == null) return;

        int idx = Arrays.asList(conNames).indexOf(sel);

        String wStr = JOptionPane.showInputDialog(this, "Enter new weight:");
        if (wStr == null) return;

        try {
            connections.get(idx).w = Double.parseDouble(wStr);
            computeMSTAndRefresh();
        } catch (Exception ignored) {
        }
    }

    private void deleteConnectionDialog() {
        if (connections.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No connections.");
            return;
        }

        String[] conNames = connections.stream()
                .map(c -> c.a + " - " + c.b + " : " + c.w)
                .toArray(String[]::new);

        String sel = (String) JOptionPane.showInputDialog(this, "Select:",
                "Delete Connection", JOptionPane.PLAIN_MESSAGE, null, conNames, conNames[0]);

        if (sel == null) return;

        int idx = Arrays.asList(conNames).indexOf(sel);
        connections.remove(idx);

        computeMSTAndRefresh();
    }

    private void changeSourceDialog() {
        if (buildings.isEmpty()) return;

        String[] names = buildings.keySet().toArray(new String[0]);
        String sel = (String) JOptionPane.showInputDialog(this, "Select source:", "Source",
                JOptionPane.PLAIN_MESSAGE, null, names, sourceBuilding);

        if (sel == null) return;

        sourceBuilding = sel;
        sourceLabel.setText("Source: " + sel);
        graphPanel.repaint();
    }

    private List<Conn> computeKruskalMST() {
        List<Conn> sorted = new ArrayList<>(connections);
        sorted.sort(Comparator.comparingDouble(c -> c.w));

        Map<String, String> parent = new HashMap<>();
        for (String b : buildings.keySet()) parent.put(b, b);

        java.util.function.Function<String, String> find =
                new java.util.function.Function<>() {
                    @Override
                    public String apply(String x) {
                        return parent.get(x).equals(x) ? x : apply(parent.get(x));
                    }
                };

        List<Conn> result = new ArrayList<>();

        for (Conn c : sorted) {
            if (!buildings.containsKey(c.a) || !buildings.containsKey(c.b))
                continue;

            String ra = find.apply(c.a);
            String rb = find.apply(c.b);

            if (!ra.equals(rb)) {
                result.add(c);
                parent.put(ra, rb);
            }
        }
        return result;
    }

    private void computeMSTAndRefresh() {
        mst = computeKruskalMST();
        double sum = mst.stream().mapToDouble(c -> c.w).sum();
        costLabel.setText("Total MST Cost: " + sum);
        graphPanel.repaint();
    }

    private void saveToTextFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            for (var e : buildings.entrySet())
                pw.println("B|" + e.getKey() + "|" + e.getValue().x + "|" + e.getValue().y);

            for (Conn c : connections)
                pw.println("C|" + c.a + "|" + c.b + "|" + c.w);

            pw.println("S|" + sourceBuilding);

            JOptionPane.showMessageDialog(this, "Saved.");
        } catch (Exception ex) {
        }
    }

    private void loadFromTextFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(fc.getSelectedFile()))) {
            buildings.clear();
            connections.clear();

            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");

                if (p[0].equals("B"))
                    buildings.put(p[1], new Point(Integer.parseInt(p[2]), Integer.parseInt(p[3])));
                else if (p[0].equals("C"))
                    connections.add(new Conn(p[1], p[2], Double.parseDouble(p[3])));
                else if (p[0].equals("S"))
                    sourceBuilding = p.length > 1 ? p[1] : null;
            }

            computeMSTAndRefresh();
            JOptionPane.showMessageDialog(this, "Loaded.");
        } catch (Exception ex) {
        }
    }

    // ================= GRAPH PANEL ================
    private class GraphPanel extends JPanel {

        private final int R = 22;
        private String draggingName = null;
        private int dragX, dragY;

        GraphPanel() {
            setBackground(Color.WHITE);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    draggingName = findBuildingAt(e.getX(), e.getY());
                    if (draggingName != null) {
                        Point p = buildings.get(draggingName);
                        dragX = e.getX() - p.x;
                        dragY = e.getY() - p.y;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggingName != null) {
                        buildings.put(draggingName, new Point(e.getX() - dragX, e.getY() - dragY));
                        computeMSTAndRefresh();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggingName = null;
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private String findBuildingAt(int x, int y) {
            for (var e : buildings.entrySet()) {
                Point p = e.getValue();
                if ((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) <= R * R) return e.getKey();
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            // Background map
            g2.drawImage(campusMapImg, 0, 0, getWidth(), getHeight(), null);

            // All connections
            for (Conn c : connections) {
                Point a = buildings.get(c.a);
                Point b = buildings.get(c.b);
                g2.setColor(Color.GRAY);
                g2.drawLine(a.x, a.y, b.x, b.y);

                int mx = (a.x + b.x) / 2;
                int my = (a.y + b.y) / 2;

                g2.setColor(Color.BLUE);
                g2.drawString(String.format("%.1f", c.w), mx + 5, my - 5);
            }

            // MST edges
            g2.setStroke(new BasicStroke(4));
            g2.setColor(new Color(0, 150, 0));
            for (Conn c : mst) {
                Point a = buildings.get(c.a);
                Point b = buildings.get(c.b);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            // ⭐ Bigger building name font
            g2.setFont(new Font("Arial", Font.BOLD, 16));

            // Draw buildings
            for (var e : buildings.entrySet()) {
                Point p = e.getValue();

                // ⭐ Draw icon
                g2.drawImage(buildingIcon, p.x - R, p.y - R, R * 2, R * 2, null);

                // Yellow ring for source
                if (e.getKey().equals(sourceBuilding)) {
                    g2.setColor(Color.YELLOW);
                    g2.drawOval(p.x - R - 2, p.y - R - 2, R * 2 + 4, R * 2 + 4);
                }

                // Building name
                g2.setColor(Color.BLACK);
                g2.drawString(e.getKey(), p.x - 20, p.y + R + 20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusWiFiNetworkk().setVisible(true));
    }
}
