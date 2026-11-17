import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class SpaceDodger extends JPanel implements ActionListener, KeyListener {

    // Game Window Dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // Game Loop Timer (60 FPS)
    private Timer timer;
    private boolean isRunning = false;
    private boolean isGameOver = false;

    // Player Properties
    private int playerX = WIDTH / 2 - 20;
    private int playerY = HEIGHT - 100;
    private final int playerWidth = 40;
    private final int playerHeight = 45;
    private int playerSpeed = 7;
    private boolean moveLeft = false;
    private boolean moveRight = false;

    // Game Objects
    private ArrayList<Star> stars;
    private ArrayList<Asteroid> asteroids;
    private ArrayList<Particle> particles;
    private Random random;

    // Score & Difficulty
    private int score = 0;
    private int highScore = 0;
    private int spawnCounter = 0;
    private int difficultyRate = 35; // Lower = more asteroids

    public SpaceDodger() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(10, 12, 28)); // Deep space dark blue
        setFocusable(true);
        addKeyListener(this);

        random = new Random();
        stars = new ArrayList<>();
        asteroids = new ArrayList<>();
        particles = new ArrayList<>();

        // Initialize background stars
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(3) + 1));
        }

        timer = new Timer(16, this); // ~60 Frames Per Second
        timer.start();
    }

    // Reset game state to start/restart
    private void startGame() {
        playerX = WIDTH / 2 - 20;
        asteroids.clear();
        particles.clear();
        score = 0;
        difficultyRate = 35;
        isGameOver = false;
        isRunning = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateStars();

        if (isRunning && !isGameOver) {
            updatePlayer();
            updateAsteroids();
            score++;

            // Gradually increase difficulty
            if (score % 500 == 0 && difficultyRate > 12) {
                difficultyRate -= 3;
            }
        }

        updateParticles();
        repaint(); // Triggers paintComponent to redraw the screen
    }

    private void updateStars() {
        for (Star star : stars) {
            star.y += star.speed;
            if (star.y > HEIGHT) {
                star.y = 0;
                star.x = random.nextInt(WIDTH);
            }
        }
    }

    private void updatePlayer() {
        if (moveLeft && playerX > 10) {
            playerX -= playerSpeed;
        }
        if (moveRight && playerX < WIDTH - playerWidth - 10) {
            playerX += playerSpeed;
        }

        // Create engine thruster particles
        if (random.nextBoolean()) {
            particles.add(new Particle(
                playerX + playerWidth / 2 + random.nextInt(10) - 5,
                playerY + playerHeight,
                (random.nextDouble() - 0.5) * 2,
                random.nextDouble() * 3 + 2,
                new Color(0, 220, 255),
                15
            ));
        }
    }

    private void updateAsteroids() {
        spawnCounter++;
        if (spawnCounter >= difficultyRate) {
            int size = random.nextInt(35) + 25;
            int x = random.nextInt(WIDTH - size);
            int speed = random.nextInt(4) + 4 + (score / 1000);
            asteroids.add(new Asteroid(x, -size, size, speed));
            spawnCounter = 0;
        }

        Rectangle playerBounds = new Rectangle(playerX + 5, playerY + 5, playerWidth - 10, playerHeight - 10);

        Iterator<Asteroid> it = asteroids.iterator();
        while (it.hasNext()) {
            Asteroid ast = it.next();
            ast.y += ast.speed;
            ast.angle += ast.rotationSpeed;

            // Check Collision
            Rectangle astBounds = new Rectangle(ast.x + 4, ast.y + 4, ast.size - 8, ast.size - 8);
            if (playerBounds.intersects(astBounds)) {
                triggerExplosion(playerX + playerWidth / 2, playerY + playerHeight / 2);
                isGameOver = true;
                if (score > highScore) {
                    highScore = score;
                }
            }

            // Remove off-screen asteroids
            if (ast.y > HEIGHT) {
                it.remove();
            }
        }
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }
    }

    private void triggerExplosion(int x, int y) {
        Color[] colors = {Color.ORANGE, Color.RED, Color.YELLOW, Color.WHITE};
        for (int i = 0; i < 60; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 6 + 1;
            particles.add(new Particle(
                x, y,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                colors[random.nextInt(colors.length)],
                random.nextInt(30) + 20
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable Anti-Aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Starfield
        for (Star star : stars) {
            g2d.setColor(new Color(255, 255, 255, star.speed * 70));
            g2d.fillOval(star.x, star.y, star.speed, star.speed);
        }

        // 2. Draw Particles
        for (Particle p : particles) {
            int alpha = Math.max(0, Math.min(255, p.life * 8));
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g2d.fillOval((int) p.x, (int) p.y, 5, 5);
        }

        // 3. Draw Player Spaceship
        if (!isGameOver) {
            drawSpaceship(g2d, playerX, playerY, playerWidth, playerHeight);
        }

        // 4. Draw Asteroids
        for (Asteroid ast : asteroids) {
            drawAsteroid(g2d, ast);
        }

        // 5. Draw HUD & Menus
        drawUI(g2d);
    }

    private void drawSpaceship(Graphics2D g2d, int x, int y, int w, int h) {
        // Ship Body (Neon Cyan & White)
        int[] xPoints = {x + w / 2, x + w, x + w / 2, x};
        int[] yPoints = {y, y + h, y + h - 10, y + h};

        g2d.setColor(new Color(0, 240, 255));
        g2d.fillPolygon(xPoints, yPoints, 4);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(xPoints, yPoints, 4);

        // Cockpit Window
        g2d.setColor(new Color(15, 25, 60));
        g2d.fillOval(x + w / 2 - 5, y + 14, 10, 14);
    }

    private void drawAsteroid(Graphics2D g2d, Asteroid ast) {
        Graphics2D gCopy = (Graphics2D) g2d.create();
        gCopy.translate(ast.x + ast.size / 2.0, ast.y + ast.size / 2.0);
        gCopy.rotate(ast.angle);

        // Glowing Neon Magenta/Pink Asteroid
        gCopy.setColor(new Color(45, 20, 50));
        gCopy.fillRoundRect(-ast.size / 2, -ast.size / 2, ast.size, ast.size, 15, 15);

        gCopy.setColor(new Color(255, 50, 150));
        gCopy.setStroke(new BasicStroke(2.5f));
        gCopy.drawRoundRect(-ast.size / 2, -ast.size / 2, ast.size, ast.size, 15, 15);

        gCopy.dispose();
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("SCORE: " + score, 20, 30);
        g2d.drawString("HIGH SCORE: " + highScore, WIDTH - 180, 30);

        if (!isRunning) {
            drawCenteredOverlay(g2d, "NEON SPACE DODGER", "Use LEFT / RIGHT Arrows or A / D to Move", "Press SPACE to Launch");
        } else if (isGameOver) {
            drawCenteredOverlay(g2d, "SHIP DESTROYED!", "Final Score: " + score, "Press SPACE to Play Again");
        }
    }

    private void drawCenteredOverlay(Graphics2D g2d, String title, String subtitle, String actionText) {
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(0, 240, 255));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 44));
        drawCenteredString(g2d, title, HEIGHT / 2 - 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCenteredString(g2d, subtitle, HEIGHT / 2 + 5);

        g2d.setColor(new Color(255, 50, 150));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        drawCenteredString(g2d, actionText, HEIGHT / 2 + 55);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }

    // Keyboard Input Handling
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) moveLeft = true;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) moveRight = true;

        if (key == KeyEvent.VK_SPACE && (!isRunning || isGameOver)) {
            startGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) moveLeft = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) moveRight = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Helper Classes for Game Entities
    private static class Star {
        int x, y, speed;
        Star(int x, int y, int speed) { this.x = x; this.y = y; this.speed = speed; }
    }

    private static class Asteroid {
        int x, y, size, speed;
        double angle = 0;
        double rotationSpeed = 0.05;
        Asteroid(int x, int y, int size, int speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        Color color;
        int life;
        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color; this.life = life;
        }
    }

    // Main Entry Point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Neon Space Dodger - Java Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new SpaceDodger());
            frame.pack();
            frame.setLocationRelativeTo(null); // Center window on screen
            frame.setVisible(true);
        });
    }
}