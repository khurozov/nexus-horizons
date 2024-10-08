package main.entity;

import lombok.Getter;
import main.GameWindow;
import multiplayer.EnemyState;
import utils.AudioUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static utils.Const.rand;

/**
 * author: ahror
 * <p>
 * since: 9/2/24
 */
public class Enemy extends Entity {
    private Point2D.Float target;
    private Point2D.Float velocity;
    private final float maxSpeed;
    private final float maxForce;
    @Getter private final float size;
    private final int maxHealth;
    private float currentHealth;
    private boolean isDead;
    private final List<Point2D.Float> explosionParticles;
    private float explosionTime;

    private static final int HEALTH_BAR_DISPLAY_TIME = 850; // milliseconds
    private long lastDamageTime;
    private boolean isHealthBarVisible;

    public Enemy(UUID id) {
        super(id);
        this.velocity = new Point2D.Float(0, 0);
        this.maxSpeed = 2.5f;
        this.maxForce = 0.1f;
        this.color = new Color(rand.nextInt(150, 255), rand.nextInt(0, 80), rand.nextInt(0, 100), 120);
        this.size = rand.nextInt(30) + 5; // Random size between 15 and 25
        this.maxHealth = (int) (size * 4); // Health based on size
        this.currentHealth = maxHealth;
        this.isDead = false;
        this.explosionParticles = new ArrayList<>();

        this.lastDamageTime = 0;
        this.isHealthBarVisible = false;
        setNewTarget();
    }

    public void spawn(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!isDead) {
            drawBallWithShadowAndEye(g2d, position.x(), position.y(), color);
            if (isHealthBarVisible) {
                drawHealthBar(g2d);
            }
            drawWrappedInstances(g2d);
        } else {
            drawExplosion(g2d);
        }

        g2d.dispose();
        update();
    }

    private void drawBallWithShadowAndEye(Graphics2D g2d, float x, float y, Color color) {
        // Draw the main body of the enemy
        g2d.setColor(color);
        g2d.fillOval((int) (x - size / 2), (int) (y - size / 2), (int) size, (int) size);

        // Draw an "eye" to show the direction of movement
        float eyeSize = size / 4;
        float eyeX = x + velocity.x * (size / 2 / maxSpeed);
        float eyeY = y + velocity.y * (size / 2 / maxSpeed);
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) (eyeX - eyeSize / 2), (int) (eyeY - eyeSize / 2), (int) eyeSize, (int) eyeSize);
    }

    private void drawWrappedInstances(Graphics2D g2d) {
        if (position.x() > GameWindow.getScreenWidth() + size) {
            drawBallWithShadowAndEye(g2d, position.x() - GameWindow.getScreenWidth(), position.y(), color);
        }
        if (position.x() < 0) {
            drawBallWithShadowAndEye(g2d, position.x() + GameWindow.getScreenWidth(), position.y(), color);
        }
        if (position.y() > GameWindow.getScreenHeight() + size) {
            drawBallWithShadowAndEye(g2d, position.x(), position.y() - GameWindow.getScreenHeight(), color);
        }
        if (position.y() < 0) {
            drawBallWithShadowAndEye(g2d, position.x(), position.y() + GameWindow.getScreenHeight(), color);
        }
    }

    private void drawHealthBar(Graphics2D g2d) {
        int barWidth = (int) size;
        int barHeight = 2;
        int x = (int) (position.x() - size / 2);
        int y = (int) (position.y() - size / 2 - 10);

        // Draw background
        g2d.setColor(Color.GRAY);
        g2d.fillRect(x, y, barWidth, barHeight);

        // Draw health
        int healthWidth = (int) (currentHealth / maxHealth * barWidth);
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, healthWidth, barHeight);

        // Calculate and apply fade-out effect
        long timeSinceLastDamage = System.currentTimeMillis() - lastDamageTime;
        if (timeSinceLastDamage < HEALTH_BAR_DISPLAY_TIME) {
            float alpha = 1f - (float) timeSinceLastDamage / HEALTH_BAR_DISPLAY_TIME;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        } else {
            isHealthBarVisible = false;
        }
    }


    private void drawExplosion(Graphics2D g2d) {
        for (Point2D.Float particle : explosionParticles) {
            float alpha = 1 - (explosionTime / 1000f); // Fade out over 1 second
            if (alpha < 0) alpha = 0;
            g2d.setColor(new Color(1f, 0.5f, 0f, alpha));
            g2d.fillOval((int) particle.x, (int) particle.y, 3, 3);
        }
    }

    public void takeDamage(float damage) {
        currentHealth -= damage;
        lastDamageTime = System.currentTimeMillis();
        isHealthBarVisible = true;

        if (currentHealth <= 0 && !isDead) {
            die();
        }
    }

    private void die() {
        isDead = true;
        AudioUtils.play("die.wav");
        initializeExplosion();
    }

    private void initializeExplosion() {
        for (int i = 0; i < 50; i++) {
            float angle = rand.nextFloat() * 2 * (float) Math.PI;
            float speed = rand.nextFloat() * 2 + 1;
            float x = position.x() + (float) Math.cos(angle) * speed;
            float y = position.y() + (float) Math.sin(angle) * speed;
            explosionParticles.add(new Point2D.Float(x, y));
        }
        explosionTime = 0;
    }

    public void update() {
        if (!isDead) {
            move();
            // Update health bar visibility
            long timeSinceLastDamage = System.currentTimeMillis() - lastDamageTime;
            isHealthBarVisible = timeSinceLastDamage < HEALTH_BAR_DISPLAY_TIME;
        } else {
            updateExplosion();
        }
    }

    private void updateExplosion() {
        explosionTime += 16; // Assuming 60 FPS
        if (explosionTime > 1000) { // Explosion lasts for 1 second
            explosionParticles.clear();
        } else {
            for (Point2D.Float particle : explosionParticles) {
                particle.x += (rand.nextFloat() - 0.5f) * 2;
                particle.y += (rand.nextFloat() - 0.5f) * 2;
            }
        }
    }

    public boolean isDead() {
        return isDead && explosionParticles.isEmpty();
    }

    private void setNewTarget() {
        float targetX = rand.nextFloat() * GameWindow.getScreenWidth();
        float targetY = rand.nextFloat() * GameWindow.getScreenHeight();
        this.target = new Point2D.Float(targetX, targetY);
    }

    private void move() {
        float dx = target.x - position.x();
        float dy = target.y - position.y();
        Point2D.Float desired = new Point2D.Float(dx, dy);

        // Normalize and scale desired velocity
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < 100) { // Slow down when approaching target
            float m = map(distance, 0, 100, 0, maxSpeed);
            desired.x = desired.x / distance * m;
            desired.y = desired.y / distance * m;
        } else {
            desired.x = desired.x / distance * maxSpeed;
            desired.y = desired.y / distance * maxSpeed;
        }

        // Calculate steering force
        Point2D.Float steer = new Point2D.Float(desired.x - velocity.x, desired.y - velocity.y);
        steer = limit(steer, maxForce);

        // Apply steering force
        velocity.x += steer.x;
        velocity.y += steer.y;
        velocity = limit(velocity, maxSpeed);

        // Update position
        position.incX(velocity.x);
        position.incY(velocity.y);

        // Wrap around screen edges
        wrapPosition();

        // Add some natural variation
        addNaturalVariation();

        // Check if we've reached the target
        if (distance < 5) {
            setNewTarget();
        }
    }

    private void wrapPosition() {
        if (position.x() < 0) position.x(position.x() + GameWindow.getScreenWidth());
        if (position.x() > GameWindow.getScreenWidth()) position.x(position.x() - GameWindow.getScreenWidth());
        if (position.y() < 0) position.y(position.y() + GameWindow.getScreenHeight());
        if (position.y() > GameWindow.getScreenHeight()) position.y(position.y() - GameWindow.getScreenHeight());
    }

    private void addNaturalVariation() {
        // Add small random variations to velocity
        velocity.x += (rand.nextFloat() - 0.5f) * 0.2f;
        velocity.y += (rand.nextFloat() - 0.5f) * 0.2f;
    }

    private Point2D.Float limit(Point2D.Float vector, float max) {
        float magnitudeSquared = vector.x * vector.x + vector.y * vector.y;
        if (magnitudeSquared > max * max) {
            float magnitude = (float) Math.sqrt(magnitudeSquared);
            vector.x = vector.x / magnitude * max;
            vector.y = vector.y / magnitude * max;
        }
        return vector;
    }

    private float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }

    //multiplayer methods

    public void updateFromEnemyState(EnemyState state) {
        this.position.setCoordinates(state.getPosition().x(), state.getPosition().y());

        this.currentHealth = state.getHealth();

        this.isDead = state.isDead();
        if (this.velocity != null && state.getVelocity() != null) {
            this.velocity.setLocation(state.getVelocity().x, state.getVelocity().y);
        }

        this.target.setLocation(state.getTarget().x, state.getTarget().y);

        this.explosionTime = state.getExplosionTime();
        this.explosionParticles.clear();
        this.explosionParticles.addAll(state.getExplosionParticles());
    }

    public EnemyState createState() {
        return new EnemyState(getId(), position, currentHealth, isDead, velocity, target, explosionTime, explosionParticles);
    }

}