package ca.codepet.worlds;

import com.badlogic.gdx.math.MathUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

import ca.codepet.Plant;
import ca.codepet.Plants.Peashooter;
import ca.codepet.Zombies.BasicZombie;
import ca.codepet.Zombies.BucketheadZombie;
import ca.codepet.Zombies.Zombie;
import ca.codepet.ui.PlantBar;
import ca.codepet.GameRoot;
import ca.codepet.Lawnmower;
import ca.codepet.characters.PlantCard;
import ca.codepet.characters.Sun;
import ca.codepet.ui.PlantPicker;

public class DayWorld implements Screen {
    private Texture backgroundTexture;
    private SpriteBatch batch;
    private PlantBar plantBar;
    private Plant[][] plants;
    private ShapeRenderer shape = new ShapeRenderer();
    private PlantPicker plantPicker;
    private boolean gameStarted = false;

    
    // "final" denotes that this is a constant and cannot be reassigned
    final private int LAWN_WIDTH = 9;
    final private int LAWN_HEIGHT = 5;
    final private int LAWN_TILEWIDTH = 80;
    final private int LAWN_TILEHEIGHT = 96;
    final private int LAWN_TILEX = 56;
    final private int LAWN_TILEY = 416;

    private static final float SUN_SPAWN_RATE = 1f; // seconds
    private float sunSpawnTimer = 0f;
    // Add array to track suns
    private Array<Sun> suns = new Array<>();

    private int sunBalance = 0;

    private final GameRoot game; 

    // private Zombie zombie = new BasicZombie(this);
    private Array<Zombie> zombies = new Array<>();
    private Array<Lawnmower> lawnmowers = new Array<>();

    private float waveTimer = 0f;
    private float timeBetweenWaves = 10f; // seconds

    public DayWorld(GameRoot game) {
        this.game = game;

    }
    @Override
    public void show() {
        addZombie(new BasicZombie(this));
        
        for(int i = 0; i < LAWN_HEIGHT; i++) {
            lawnmowers.add(new Lawnmower(this, i));
        }
        
        // Load the background texture
        backgroundTexture = new Texture("backgrounds/day.png");
        batch = new SpriteBatch();
        plantBar = new PlantBar(sunBalance);
        plants = new Plant[LAWN_HEIGHT][LAWN_WIDTH];
        for (int y = 0; y < LAWN_HEIGHT; y++) {
            for (int x = 0; x < LAWN_WIDTH; x++) {
                plants[y][x] = null;
            }
        }
        plantPicker = new PlantPicker(plantBar);
    }

    @Override
    public void render(float delta) {
        // Draw the background texture
        batch.begin();
        batch.draw(backgroundTexture, -200, 0);
        batch.end();

        // Draw the plant bar
        plantBar.render();

        if (!gameStarted) {
            // Render plant picker if game hasn't started
            plantPicker.render();
            if (plantPicker.isPicked()) {
                gameStarted = true;
            }

            if (Gdx.input.justTouched()) {
                float mouseX = Gdx.input.getX();
                float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
                PlantCard clickedCard = plantBar.checkCardClick(mouseX, mouseY);
                if (clickedCard != null) {
                    plantPicker.returnCard(clickedCard.getPlantType());
                }
            }
            
            // Don't continue the game until picked
            return;
        }

        batch.begin();
        // Draw plants
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        int clickedTileX = MathUtils.floor((mouseX - LAWN_TILEX) / LAWN_TILEWIDTH);
        int clickedTileY = -MathUtils.floor((mouseY - LAWN_TILEY) / LAWN_TILEHEIGHT);
        if (Gdx.input.justTouched()) {
            if (clickedTileX >= 0 
            && clickedTileX < LAWN_WIDTH 
            && clickedTileY >= 0 
            && clickedTileY < LAWN_HEIGHT) {
                if (plants[clickedTileY][clickedTileX] == null) {
                    float pX = LAWN_TILEX + clickedTileX * LAWN_TILEWIDTH + LAWN_TILEWIDTH / 2;
                    float pY = LAWN_TILEY - clickedTileY * LAWN_TILEHEIGHT + LAWN_TILEHEIGHT / 2;
                    plants[clickedTileY][clickedTileX] = new Peashooter(pX, pY);
                }
                else {
                    // Add null check before disposing
                    Plant plant = plants[clickedTileY][clickedTileX];
                    if (plant != null) {
                        plant.dispose();
                        plants[clickedTileY][clickedTileX] = null;
                    }
                }
            }
        }
        for (int y = 0; y < LAWN_HEIGHT; y++) {
            for (int x = 0; x < LAWN_WIDTH; x++) {
                Plant p = plants[y][x];
                if (p != null) {
                    p.update(delta);
                    p.render(batch);
                } else if (x == clickedTileX && y == clickedTileY) {
                    // Draw "ghost" of plant here
                }
            }
        }

        // Update sun spawning
        sunSpawnTimer += delta;
        if (sunSpawnTimer >= SUN_SPAWN_RATE) {
            sunSpawnTimer = 0f;
            // Create new sun and add to array
            suns.add(new Sun());
        }

        // Render all suns and check for collection
        // Loop through suns
        for (int i = 0; i < suns.size; i++) {
            Sun sun = suns.get(i);
            // Check if the sun was collected
            if (sun.checkClick()) {
                // Add 25 balance
                sunBalance += 25;
                plantBar.setSunDisplay(sunBalance);
                // Remove sun from array and dispose
                suns.removeIndex(i);
                sun.dispose();
                // Since we've removed our current index, decrement i
                i--;
            } else if (!sun.isAlive()) {
                // If the sun is dead (expired), remove and dispose
                suns.removeIndex(i);
                sun.dispose();
                // Since we've removed our current index, decrement i
                i--;
            } else {
                // Render the sun
                sun.render(batch);
            }
        }

        // Update wave timer
        waveTimer += delta;
        if (waveTimer >= timeBetweenWaves) {
            waveTimer = 0f;
            spawnWave();
        }

        renderLawnmower();
        renderZombie(delta);

        batch.end();

        // Draw the plant bar
        plantBar.render();
    }

    private void renderLawnmower() {
        for(Lawnmower lawnmower : lawnmowers) {
            if(lawnmower.getIsActivated()) {
                lawnmower.move();
            }
            batch.draw(lawnmower.getTextureRegion(), lawnmower.getX(), lawnmower.getY(), lawnmower.getWidth(), lawnmower.getHeight());
        }
        
    }

    private void renderZombie(float delta) {
        for(Zombie zombie : zombies) {

            if(zombie.getCol() < 0) {
                if(lawnmowers.get(LAWN_HEIGHT - zombie.getRow() - 1) == null) {
                    removeZombie(zombie);
                    goEndscreen();
                } else {
                    lawnmowers.get(LAWN_HEIGHT - zombie.getRow() - 1).activate();
                }
                
                break;
                // TODO end screen go there
            }
            
            // System.out.println(zombie.getRow());
            
            batch.draw(zombie.getTextureRegion(), 
                      zombie.getX(), 
                      (LAWN_HEIGHT - zombie.getRow()) * LAWN_TILEHEIGHT - (zombie.getHeight() - LAWN_TILEHEIGHT)/2,
                      zombie.getWidth(),
                      zombie.getHeight());


            Plant plant = plants[zombie.getRow()][zombie.getCol()];


            zombie.update(delta);
            
            if(plant != null) {
                if(zombie.canAttack()) {  // Only attack if cooldown is ready
                    zombie.attack(plant);
                    if(plant.isDead()) plants[zombie.getRow()][zombie.getCol()] = null;
                }
            } else {
                zombie.move();
            }
        }
    }

    public void addZombie(Zombie zombie) {
        System.out.println(3333);
        zombies.add(zombie);
    }
    
    public void removeZombie(Zombie zombie) {
        zombie.dispose();
        zombies.removeValue(zombie, true);
    }

    public void goEndscreen() {
        // game.setScreen(new EndScreen(game));
    }

    private void spawnWave() {
        int numberOfZombies = 5; // Number of zombies per wave
        for (int i = 0; i < numberOfZombies; i++) {
            addZombie(new BucketheadZombie(this));
        }
    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    public int getLawnHeight() {
        return LAWN_HEIGHT;
    }

    public int getLawnTileHeight() {
        return LAWN_TILEHEIGHT;
    }

    public int getLawnTileWidth() {
        return LAWN_TILEWIDTH;
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        backgroundTexture.dispose();
        batch.dispose();
        plantBar.dispose();
        // Dispose suns
        for(Sun sun : suns) {
            sun.dispose();
        }

        for(Zombie zombie : zombies) {
            zombie.dispose();
        }
        zombies.clear();
    }
}