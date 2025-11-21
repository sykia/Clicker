import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class Clicker extends JFrame {

    private static final String APP_TITLE = "CLICKER";
    private static final String VERSION = "v1.5";
    private static final String TG_LINK = "https://t.me/shlu4a";
    private static final String TG_TEXT = "t.me/shlu4a";

    private final Color C_BG = new Color(15, 15, 20);
    private final Color C_PANEL = new Color(30, 30, 35);
    private final Color C_ACCENT = new Color(255, 255, 255);
    private final Color C_TEXT = new Color(230, 230, 230);
    private final Color C_DIM = new Color(120, 120, 120);

    private Robot robot;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread clickThread;
    private Preferences prefs;

    private int delayMs = 100;
    private int mouseMask = InputEvent.BUTTON1_DOWN_MASK;
    private int customKeyCode = -1;
    private boolean useKey = false;
    private boolean onTop = false;

    private JPanel mainContainer;
    private CardLayout cardLayout;
    private JLabel statusLabel;
    private TitanButton toggleBtn;
    private Point initialClick;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Clicker().setVisible(true));
    }

    public Clicker() {
        try { robot = new Robot(); } catch (Exception e) { e.printStackTrace(); }
        prefs = Preferences.userNodeForPackage(Clicker.class);
        loadSettings();

        setUndecorated(true);
        setTitle(APP_TITLE);
        setSize(350, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(new Color(0,0,0,0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(new Color(50,50,60));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);

                g2.setColor(C_DIM);
                g2.drawLine(getWidth()-12, getHeight()-5, getWidth()-5, getHeight()-12);
                g2.drawLine(getWidth()-8, getHeight()-5, getWidth()-5, getHeight()-8);
            }
        };
        setContentPane(root);

        ResizeListener resizeListener = new ResizeListener();
        root.addMouseListener(resizeListener);
        root.addMouseMotionListener(resizeListener);

        initHeader(root);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setOpaque(false);

        mainContainer.add(createHomePanel(), "HOME");
        mainContainer.add(createSettingsPanel(), "SETTINGS");
        root.add(mainContainer, BorderLayout.CENTER);
    }

    private void loadSettings() {
        delayMs = prefs.getInt("delay", 100);
        mouseMask = prefs.getInt("mouseMask", InputEvent.BUTTON1_DOWN_MASK);
        useKey = prefs.getBoolean("useKey", false);
        customKeyCode = prefs.getInt("customKey", -1);
        onTop = prefs.getBoolean("onTop", false);
        setAlwaysOnTop(onTop);
    }

    private void saveSettings() {
        prefs.putInt("delay", delayMs);
        prefs.putInt("mouseMask", mouseMask);
        prefs.putBoolean("useKey", useKey);
        prefs.putInt("customKey", customKeyCode);
        prefs.putBoolean("onTop", onTop);
    }

    private void initHeader(JPanel root) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(10, 15, 0, 15));

        JLabel title = new JLabel(APP_TITLE + " " + VERSION);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(C_DIM);

        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(C_TEXT);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(null);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            saveSettings(); // Сохраняем перед выходом
            System.exit(0);
        });

        header.add(title, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);

        MouseAdapter dragListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - initialClick.x,
                        getLocation().y + e.getY() - initialClick.y);
            }
        };
        header.addMouseListener(dragListener);
        header.addMouseMotionListener(dragListener);

        root.add(header, BorderLayout.NORTH);
    }

    private JPanel createHomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(40, 20, 40, 20));

        statusLabel = new JLabel("IDLE");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);

        toggleBtn = new TitanButton("START");
        toggleBtn.setPreferredSize(new Dimension(180, 60));
        toggleBtn.setAlignmentX(CENTER_ALIGNMENT);
        toggleBtn.addActionListener(e -> toggleClicker());

        JButton settingsBtn = new JButton("SETTINGS");
        settingsBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        settingsBtn.setForeground(C_DIM);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setBorder(BorderFactory.createMatteBorder(0,0,1,0, C_DIM));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settingsBtn.setAlignmentX(CENTER_ALIGNMENT);
        settingsBtn.addActionListener(e -> cardLayout.show(mainContainer, "SETTINGS"));

        p.add(Box.createVerticalGlue());
        p.add(statusLabel);
        p.add(Box.createVerticalGlue());
        p.add(toggleBtn);
        p.add(Box.createVerticalStrut(30));
        p.add(settingsBtn);

        return p;
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10, 20, 20, 20));

        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        JButton back = new JButton("← BACK");
        back.setForeground(C_TEXT);
        back.setFont(new Font("Segoe UI", Font.BOLD, 12));
        back.setContentAreaFilled(false);
        back.setBorder(null);
        back.setCursor(new Cursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> {
            saveSettings(); // Сохраняем при выходе из настроек
            cardLayout.show(mainContainer, "HOME");
        });
        nav.add(back, BorderLayout.WEST);
        p.add(nav, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.add(Box.createVerticalStrut(15));

        JPanel speedRow = createRowPanel();
        JLabel valLbl = new JLabel(delayMs + " ms");
        valLbl.setForeground(C_ACCENT);
        valLbl.setPreferredSize(new Dimension(50, 20));
        valLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        JSlider slider = new JSlider(1, 500, delayMs);
        slider.setOpaque(false);
        slider.setUI(new TitanSliderUI(slider));
        slider.addChangeListener(e -> {
            delayMs = slider.getValue();
            valLbl.setText(delayMs + " ms");
        });

        speedRow.add(new JLabel("Speed:"){{setForeground(C_DIM);}}, BorderLayout.WEST);
        speedRow.add(slider, BorderLayout.CENTER);
        speedRow.add(valLbl, BorderLayout.EAST);
        addCard(list, speedRow);

        JPanel modeRow = createRowPanel();
        String[] opts = {"Left Click", "Right Click", "Custom Key"};
        JComboBox<String> box = new JComboBox<>(opts);
        box.setUI(new TitanComboUI());
        if(useKey) box.setSelectedIndex(2);
        else box.setSelectedIndex(mouseMask == InputEvent.BUTTON3_DOWN_MASK ? 1 : 0);

        TitanButton keyBtn = new TitanButton("KEY");
        keyBtn.setPreferredSize(new Dimension(50, 25));
        keyBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        keyBtn.setVisible(useKey);
        if(customKeyCode != -1) keyBtn.setText(KeyEvent.getKeyText(customKeyCode));
        keyBtn.addActionListener(e -> bindKey(keyBtn));

        box.addActionListener(e -> {
            int i = box.getSelectedIndex();
            if (i == 0) { mouseMask = InputEvent.BUTTON1_DOWN_MASK; useKey = false; }
            else if (i == 1) { mouseMask = InputEvent.BUTTON3_DOWN_MASK; useKey = false; }
            else { useKey = true; }
            keyBtn.setVisible(useKey);
            modeRow.revalidate();
        });

        JPanel right = new JPanel(new BorderLayout(5,0));
        right.setOpaque(false);
        right.add(box, BorderLayout.CENTER);
        right.add(keyBtn, BorderLayout.EAST);

        modeRow.add(new JLabel("Mode:"){{setForeground(C_DIM);}}, BorderLayout.WEST);
        modeRow.add(right, BorderLayout.CENTER);
        addCard(list, modeRow);

        JPanel topRow = createRowPanel();
        JCheckBox cb = new JCheckBox();
        cb.setOpaque(false);
        cb.setSelected(onTop);
        cb.setIcon(new CheckIcon(false));
        cb.setSelectedIcon(new CheckIcon(true));
        cb.addActionListener(e -> {
            onTop = cb.isSelected();
            setAlwaysOnTop(onTop);
        });
        topRow.add(new JLabel("Always On Top"){{setForeground(C_TEXT);}}, BorderLayout.CENTER);
        topRow.add(cb, BorderLayout.EAST);
        addCard(list, topRow);

        p.add(list, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridBagLayout()); // GridBag центрирует идеально
        footer.setOpaque(false);

        JLabel link = new JLabel("<html><u>" + TG_TEXT + "</u></html>");
        link.setForeground(C_DIM);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try { Desktop.getDesktop().browse(new URI(TG_LINK)); } catch (Exception ex) {}
            }
        });

        footer.add(link);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createRowPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        return p;
    }

    private void addCard(JPanel parent, Component c) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_PANEL);
        card.setBorder(new EmptyBorder(10, 15, 10, 15));
        card.add(c);

        JPanel wrap = new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PANEL);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(2,2,2,2));
        wrap.add(card);
        wrap.setMaximumSize(new Dimension(2000, 50));

        parent.add(wrap);
        parent.add(Box.createVerticalStrut(10));
    }

    private void bindKey(TitanButton b) {
        b.setText("...");
        b.setInverted(true);
        KeyAdapter ka = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                customKeyCode = e.getKeyCode();
                b.setText(KeyEvent.getKeyText(customKeyCode));
                b.setInverted(false);
                b.removeKeyListener(this);
            }
        };
        b.addKeyListener(ka);
        b.requestFocusInWindow();
    }

    private void toggleClicker() {
        if (isRunning.get()) {
            isRunning.set(false);
            toggleBtn.setText("START");
            toggleBtn.setInverted(false);
            statusLabel.setText("IDLE");
            statusLabel.setForeground(Color.DARK_GRAY);
        } else {
            if(useKey && customKeyCode == -1) {
                cardLayout.show(mainContainer, "SETTINGS");
                return;
            }

            new Thread(() -> {
                try {
                    for(int i=3; i>0; i--) {
                        final int c = i;
                        SwingUtilities.invokeLater(() -> {
                            toggleBtn.setText(String.valueOf(c));
                            toggleBtn.setEnabled(false);
                        });
                        Thread.sleep(1000);
                    }
                } catch(Exception ignored){}

                SwingUtilities.invokeLater(() -> {
                    isRunning.set(true);
                    toggleBtn.setText("STOP");
                    toggleBtn.setInverted(true);
                    toggleBtn.setEnabled(true);
                    statusLabel.setText("ACTIVE");
                    statusLabel.setForeground(C_ACCENT);
                    startEngine();
                });
            }).start();
        }
    }

    private void startEngine() {
        clickThread = new Thread(() -> {
            while(isRunning.get()) {
                try {
                    if(useKey) {
                        robot.keyPress(customKeyCode);
                        robot.keyRelease(customKeyCode);
                    } else {
                        robot.mousePress(mouseMask);
                        robot.mouseRelease(mouseMask);
                    }
                    if (delayMs > 0) Thread.sleep(delayMs);
                } catch (Exception e) { break; }
            }
        });
        clickThread.start();
    }

    class ResizeListener extends MouseAdapter {
        private boolean isResizing = false;
        private Point startPos = null;

        public void mousePressed(MouseEvent e) {
            if (e.getX() > getWidth() - 15 && e.getY() > getHeight() - 15) {
                isResizing = true;
                startPos = e.getPoint();
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (isResizing) {
                int dx = e.getX() - startPos.x;
                int dy = e.getY() - startPos.y;
                setSize(getWidth() + dx, getHeight() + dy);
                startPos = e.getPoint();
                revalidate();
                repaint();
            }
        }

        public void mouseReleased(MouseEvent e) { isResizing = false; }

        public void mouseMoved(MouseEvent e) {
            if (e.getX() > getWidth() - 15 && e.getY() > getHeight() - 15)
                setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
            else
                setCursor(Cursor.getDefaultCursor());
        }
    }

    class TitanButton extends JButton {
        private boolean inv = false;
        public TitanButton(String t) { super(t); setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setForeground(C_TEXT); setFont(new Font("Segoe UI", Font.BOLD, 14)); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        public void setInverted(boolean i) { inv=i; setForeground(i?C_BG:C_TEXT); repaint(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(inv ? C_ACCENT : C_PANEL);
            if (!inv && getModel().isRollover()) g2.setColor(new Color(60,60,60));
            g2.fillRoundRect(0,0,getWidth(),getHeight(),15,15);
            super.paintComponent(g);
        }
    }

    class TitanSliderUI extends BasicSliderUI {
        public TitanSliderUI(JSlider b) { super(b); }
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D)g; g2.setColor(new Color(60,60,60));
            g2.fillRoundRect(trackRect.x, trackRect.y+trackRect.height/2-2, trackRect.width, 4, 4, 4);
        }
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(C_ACCENT); g2.fillOval(thumbRect.x, thumbRect.y, 16, 16);
        }
    }

    class TitanComboUI extends BasicComboBoxUI {
        protected JButton createArrowButton() { JButton b = new JButton("▼"); b.setBorder(null); b.setContentAreaFilled(false); b.setForeground(C_DIM); return b; }
        public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean h) { Graphics2D g2 = (Graphics2D)g; g2.setColor(C_BG); g2.fillRoundRect(b.x,b.y,b.width,b.height,5,5); }
        protected ListCellRenderer createRenderer() { return new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l,v,i,s,f); setBackground(s?new Color(60,60,60):C_BG); setForeground(C_TEXT); return this;
            }
        };}
    }

    class CheckIcon implements Icon {
        boolean sel;
        public CheckIcon(boolean s){sel=s;}
        public int getIconWidth(){return 16;}
        public int getIconHeight(){return 16;}
        public void paintIcon(Component c, Graphics g, int x, int y){
            Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if(sel) { g2.setColor(C_ACCENT); g2.fillRoundRect(x,y,16,16,4,4); }
            else { g2.setColor(C_DIM); g2.drawRoundRect(x,y,16,16,4,4); }
        }
    }
}