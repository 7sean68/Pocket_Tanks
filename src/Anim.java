
import com.sun.opengl.util.*;
import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.media.opengl.*;
import javax.swing.*;

import java.util.BitSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.glu.GLU;

public class Anim extends JFrame {

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Anim.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        new Anim();
    }
    GLCanvas glcanvas;
    AnimGLEventListener listener;
    Animator animator;

    public Anim() {
        listener = new AnimGLEventListener();
        glcanvas = new GLCanvas();
        glcanvas.addGLEventListener(listener);
        glcanvas.addKeyListener(listener);
        glcanvas.addMouseListener(listener);
        glcanvas.addMouseMotionListener(listener);
        getContentPane().add(glcanvas, BorderLayout.CENTER);
        animator = new FPSAnimator(30);
        animator.add(glcanvas);
        animator.start();
        setTitle("Pocket Tanks");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1015, 639);
        setLocationRelativeTo(null);
        setVisible(true);
        setFocusable(true);
        glcanvas.requestFocus();
    }
}

enum Phase {

    MainMenu, StartingGame, PauseMenu, Player1,Help, Player2, AI, HighScore, AIlevel, End
}

class AnimGLEventListener implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {

    boolean isFired, ai_or_2 = false, isEntered = false,isPrinted = false;
    Phase state = Phase.MainMenu;
    final int maxWidth = 1000, maxHeight = 600;
    TextRenderer t = new TextRenderer(Font.decode("PLAIN"));
    int x1 = 50, y1, x2 = 950, y2, th1, th2, s1 = 0, s2 = 0, ediff = 0,turns = 10;
    double vx, vy, vb = 1, bx, by, vb1 = 1, vb2 = 1, p1, p2 = 0,mv1 = 100, mv2 = 100;
    String player1 = "", player2 = "";
    ArrayList<String> s = new ArrayList<>();
    //int[][] pos = new int[maxWidth][maxHeight];
    double[] hts = new double[1001];
    String textureNames[] = {"tank.png", "tank2.png", "m1.png", "Back2.png", "bullet.png"};
    TextureReader.Texture texture[] = new TextureReader.Texture[textureNames.length];
    int textures[] = new int[textureNames.length];
    /*
     5 means gun in array pos
     x and y coordinate for gun 
     */

    @Override
    public void init(GLAutoDrawable gld) {
        GL gl = gld.getGL();
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);    //This Will Clear The Background Color To Black
        GLU glu = new GLU();
        //glu.gluPerspective(60.0, 1000.0 / 600, 2.0, 20.0);
        //gl.glViewport(-500, -300, 500, 300);
        /*gl.glMatrixMode(GL.GL_PROJECTION);
         gl.glLoadIdentity();
         gl.glFrustum(-500, 500, -300, 300, 0, -2);*/
        gl.glEnable(GL.GL_TEXTURE_2D);  // Enable Texture Mapping
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glGenTextures(textureNames.length, textures, 0);

        for (int i = 0; i < textureNames.length; i++) {
            try {
                texture[i] = TextureReader.readTexture(textureNames[i], true);
                gl.glBindTexture(GL.GL_TEXTURE_2D, textures[i]);

//                mipmapsFromPNG(gl, new GLU(), texture[i]);
                new GLU().gluBuild2DMipmaps(
                        GL.GL_TEXTURE_2D,
                        GL.GL_RGBA, // Internal Texel Format,
                        texture[i].getWidth(), texture[i].getHeight(),
                        GL.GL_RGBA, // External format from image,
                        GL.GL_UNSIGNED_BYTE,
                        texture[i].getPixels() // Imagedata
                );
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
        generateGround();
        try {
            ReadHighScore();
            //gl.glEnable(gl.GL_POLYGON_SMOOTH);
        } catch (Exception ex) {
            Logger.getLogger(AnimGLEventListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void display(GLAutoDrawable gld) {
        try {
            GL gl = gld.getGL();
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);       //Clear The Screen And The Depth Buffer
            gl.glLoadIdentity();
            DrawBackground(gl);
            switch (state) {
                case MainMenu:
                    drawMain();
                    break;
                case StartingGame:
                    drawPlayer();
                    break;
                case AIlevel:
                    drawAI();
                    break;
                case HighScore:
                    drawHighScore();
                    break;
                case PauseMenu:
                    drawPause();
                    break;
                case Player1:
                    vb = vb1;
                    Play(gl);
                    break;
                case Player2:
                    vb = vb2;
                    Play(gl);
                    break;
                case Help:
                    drawHelp();
                    break;
                case End:
                    drawEnd();
            }
            /*handleKeyPress();
            
             //        DrawGraph(gl);*/
            //gl.glLineWidth(5);
        } catch (Exception ex) {
            Logger.getLogger(AnimGLEventListener.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void Play(GL gl) {
        //if(isKeyPressed(KeyEvent.VK_ESCAPE))state = Phase.PauseMenu;
        t.setColor(Color.WHITE);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);       //Clear The Screen And The Depth Buffer
        gl.glLoadIdentity();
        DrawBackground(gl);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex3d(-1, -1, -1);
        for (int i = 0; i < 1000; i++) {
            gl.glVertex3d(i / 500.0 - 1, hts[i] / 300.0, -1);
            gl.glVertex3d((i + 1) / 500.0 - 1, hts[i + 1] / 300.0, -1);
            gl.glVertex3d((i + 1) / 500.0 - 1, -1, -1);
            gl.glVertex3d((i + 1) / 500.0 - 1, -1, -1);
        }
        gl.glVertex3d(1, -1, -1);
        gl.glEnd();
        if(!isFired)
            control();
        drawvb();
        y1 = (int) hts[x1] + 15;
        y2 = (int) hts[x2] + 15;
        th1 = (int) Math.toDegrees(Math.atan((hts[x1 + 10] - hts[x1 - 10]) / 10.0));
        th2 = (int) Math.toDegrees(Math.atan((hts[x2 + 10] - hts[x2 - 10]) / 10.0));
        drawTank(gl, x1, y1, th1, p1, 1); // -45 -> 45, 0 -> 90
        drawTank(gl, x2, y2, th2, p2, 2); // -45 -> 45, 0 -> 90
        if (isKeyPressed(KeyEvent.VK_SPACE) && !isFired) {
            //System.out.println(vb);
            switch(state){
                case Player1:
                    vx = vb * Math.cos(Math.toRadians(p1));
                    vy = vb * Math.sin(Math.toRadians(p1));
                    bx = x1 + 7 + 40 * vx / vb;
                    by = y1 + 7 + 40 * vy / vb;
                    state = Phase.Player2;
                    break;
                case Player2:
                    vx = vb * Math.cos(Math.toRadians(180 - p2));
                    vy = vb * Math.sin(Math.toRadians(p2));
                    bx = x2 - 7 + 40 * vx / vb;
                    by = y2 + 5 + 40 * vy / vb;
                    state = Phase.Player1;
            }
            isFired = true;
        }
        if (isFired) {
            DrawSprite(gl, (int) bx, (int) by, 4, 0, 0.1f);
            bx += vx;
            by += vy;
            vy -= 1;
            if (bx > maxWidth || bx < 0) {
                turns--;
                isFired = false;
            } else if (by < hts[(int) (bx)]) {
                turns--;
                isFired = false;
                for (int i = (int) bx - 9; i < (int) bx + 10 && i < maxWidth; i++) {
                    if (hts[i] < by + Math.sqrt(100 - (i - bx) * (i - bx))) {
                        hts[i] = Math.min(hts[i], by - Math.sqrt(100 - (i - bx) * (i - bx)));
                    } else {
                        hts[i] -= Math.sqrt(100 - (i - bx) * (i - bx));
                    }
                }
            }
            if(Math.pow(by - (y2-7),2) + Math.pow(bx - (x2-7),2) < 400){
                s1 += (400 - Math.sqrt(Math.pow(by - (y2-7),2) + Math.pow(bx - (x2-7),2)))/8;
                turns--;
                isFired = false;
            }
                
            if(Math.pow(by - (y1+7),2) + Math.pow(bx - (x1+7),2) < 400){
                turns--;
                s2 += (400 - Math.sqrt(Math.pow(by - (7+y1),2) + Math.pow(bx - (x1+7),2)))/8;
                isFired = false;
            }
        }
        if(turns == 0 )state = Phase.End;
    }
    void control(){
        switch(state){
            case Player1:
                if (isKeyPressed(KeyEvent.VK_S) && mv1 > 0) {
                    x1++;
                    mv1--;
                }
                if (isKeyPressed(KeyEvent.VK_A) && mv1 > 0 && x1 > 20) {
                    x1--;
                    mv1--;
                }
                if (isKeyPressed(KeyEvent.VK_LEFT)) {
                    p1 = Math.min(p1 + 1, 90);
                }
                if (isKeyPressed(KeyEvent.VK_RIGHT)) {
                    p1 = Math.max(p1 - 1, 0);
                }
                if (isKeyPressed(KeyEvent.VK_UP)) {
                    vb = Math.min(vb + 0.5, 50);
                }
                if (isKeyPressed(KeyEvent.VK_DOWN)) {
                    vb = Math.max(vb - 0.5, 1);
                }
                vb1 = vb;
                break;
            case Player2:
                if(!ai_or_2){
                    //System.out.println((810000.0/Math.pow(x1-x2, 2)) - (2 * 900.0 * (y1-y2) / Math.pow(x1-x2, 2) ) - 1.0);
                    double dx = x1 - x2;
                    dx = (10 - ediff) * 20 * Math.random() + (dx - (10 - ediff) * 10.0);
                    vb = 30;
                    double d;
                    do{
                        vb++;
                        d = (vb * vb * vb * vb/Math.pow(dx, 2)) - (2 * 961.0 * (y1-y2) / Math.pow(dx, 2) ) - 1.0;
                    }while(d < 0);
                    if(d >= 0)p2 = - Math.toDegrees(Math.atan( vb * vb/(dx) - Math.sqrt(d)));
                    vb = vb2 = vb - 1;
                    fire();
                    return;
                }
                if (isKeyPressed(KeyEvent.VK_S) && mv2 > 0 && x2 < maxWidth - 20) {
                    x2++;
                    mv2--;
                }
                if (isKeyPressed(KeyEvent.VK_A) && mv2 > 0 ) {
                    x2--;
                    mv2--;
                }
                if (isKeyPressed(KeyEvent.VK_LEFT)) {
                    p2 = Math.max(p2 - 1, 0);
                }
                if (isKeyPressed(KeyEvent.VK_RIGHT)) {
                    p2 = Math.min(p2 + 1, 90);
                }
                if (isKeyPressed(KeyEvent.VK_UP)) {
                    vb = Math.min(vb + 0.5, 50);
                }
                if (isKeyPressed(KeyEvent.VK_DOWN)) {
                    vb = Math.max(vb - 0.5, 1);
                }
                
                vb2 = vb;
        }
    }
    void drawvb() {
        t.setColor(Color.RED);
        t.beginRendering(500, 500);
        t.draw("turn no :"+((turns+1)/2), 230, 480);
        t.draw(player1+" : "+s1, 0, 480);
        t.draw(player2+" : "+s2, 430, 480);
        t.draw("Power : " + vb1, 0, 0);
        t.draw("Power : " + vb2, 360, 0);
        t.draw("fuel : "+mv1, 70, 0);
        t.draw("fuel : "+mv2, 440, 0);
        t.endRendering();
        t.setColor(Color.WHITE);
    }

    void drawTank(GL gl, int x, int y, double d1, double d2, int n) {
        if (n == 1) {
            DrawSprite(gl, x + 7, y + 7, 2, d2, 0.7f);
            DrawSprite(gl, x, y, 0, d1, 0.7f);
        } else {
            DrawSprite(gl, x - 7, y + 5, 2, 180 - d2, 0.7f);
            DrawSprite(gl, x, y, 1, d1, 0.7f);
        }
    }

    void generateGround() {
        hts[0] = 60 * Math.random() - 200;
        for (int i = 1; i < 500; i++) {
            hts[i] = hts[i - 1] + (Math.random() - 0.3) * i / 80;
        }
        for (int i = 500; i <= 1000; i++) {
            hts[i] = hts[i - 1] - (Math.random() - 0.3) * (1000 - i) / 80;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    @Override
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    /*
     public void DrawGraph(GL gl) {
        
     for (int i = 0; i < maxWidth; i++) {
     for (int j = 0; j < maxHeight; j++) {
     if (pos[i][j] == 5) {
     DrawSprite(gl, i, j, animationIndex, 1);
     }
     }
     }
        
     }*/

    public void DrawSprite(GL gl, int x, int y, int index, double a, float scale) {
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[index]);	// Turn Blending On
        gl.glPushMatrix();
        gl.glTranslated(x / (maxWidth / 2.0) - 1, y / (maxHeight / 2.0), 0);
        gl.glScaled(0.1 * scale, 100 / 600.0 * scale, 1);
        gl.glRotated(a, 0, 0, 1);
        gl.glBegin(GL.GL_QUADS);
        // Front Face
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd();
        gl.glPopMatrix();
        gl.glDisable(GL.GL_BLEND);

    }

    public void DrawBackground(GL gl) {
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textures[3]);
        gl.glPushMatrix();
        gl.glBegin(GL.GL_QUADS);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd();
        gl.glPopMatrix();
        gl.glDisable(GL.GL_BLEND);
    }

    // KeyListener    
    public void handleKeyPress() {
        switch (state) {
            case MainMenu:
                if (isKeyPressed(KeyEvent.VK_A)) {
                    //selectedX += 20 / 150;
                }
        }
    }

    public BitSet keyBits = new BitSet(256);

    @Override
    public void keyPressed(final KeyEvent event) {
        switch (state) {
            case StartingGame:
                if (!ai_or_2) {
                    player1 += event.getKeyChar();
                } else {
                    if (isEntered) {
                        player2 += event.getKeyChar();
                    } else {
                        player1 += event.getKeyChar();
                    }
                }
            case AIlevel:
                ediff = event.getKeyChar() - 10;
        }
        if (event.getKeyCode() == KeyEvent.VK_SPACE && !isFired) {
            //System.out.println(vb);
            
        }
        int keyCode = event.getKeyCode();
        keyBits.set(keyCode);
    }
    void fire(){
        switch(state){
                case Player1:
                    vx = vb * Math.cos(Math.toRadians(p1));
                    vy = vb * Math.sin(Math.toRadians(p1));
                    bx = x1 + 7 + 40 * vx / vb;
                    by = y1 + 7 + 40 * vy / vb;
                    state = Phase.Player2;
                    break;
                case Player2:
                    vx = vb * Math.cos(Math.toRadians(180 - p2));
                    vy = vb * Math.sin(Math.toRadians(p2));
                    bx = x2 - 7 + 40 * vx / vb;
                    by = y2 + 5 + 40 * vy / vb;
                    state = Phase.Player1;
            }
            isFired = true;
    }

    @Override
    public void keyReleased(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        keyBits.clear(keyCode);
        switch (state) {
            case Player1:
            case Player2:
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    state = Phase.PauseMenu;
                }
                break;
            case PauseMenu:
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    state = Phase.Player1;
                }
        }
    }

    @Override
    public void keyTyped(final KeyEvent event) {
        // don't care 
    }

    public boolean isKeyPressed(final int keyCode) {
        return keyBits.get(keyCode);
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        /*switch(state){
         case Player1:
         if(Fired)
         }*/
    }

    @Override
    public void mousePressed(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseEntered(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseExited(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //p1 = (int)Math.toDegrees(Math.atan((((me.getY() - y1 + 20) / (me.getX() - x1 + 507.0)))));
        switch (state) {
            case Player1:
                //dx = Math.sqrt((me.getY() - 20.0 + y1 - 300.0) * (me.getY() - 20.0 + y1 - 300.0) + (me.getX() - 7.0 - x1) * (me.getX() - 7.0 - x1));
                if (me.getY() - 20.0 < 300.0 - y1 && me.getX() - 7.0 > x1) {
                    p1 = (int) Math.toDegrees(Math.atan(-(((me.getY() - 20.0 + y1 - 300.0) / (me.getX() - 7.0 - x1)))));
                }
                return;
            case Player2:
                if(!ai_or_2)return;
                if (me.getY() - 20.0 < 300.0 - y2 && me.getX() - 7.0 < x2) {
                    p2 =  (int) Math.toDegrees(Math.atan((((me.getY() - 20.0 + y2 - 300.0) / (me.getX() - 7.0 - x2)))));
                }
        }
    }

    @Override
    public void mouseMoved(MouseEvent me) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        

    }

    public void WriteHighScore() throws Exception {
        PrintWriter output = new PrintWriter("file.txt");
        for (int i = 0; i < s.size() && i < 5; i++) {
            output.println(s.get(i));
        }
        output.close();
    }

    public void ReadHighScore() throws Exception {
        s.clear();
        Scanner input = new Scanner(new File("file.txt"));
        while (input.hasNext()) 
            s.add(input.nextLine());
    }

    public void drawMain() throws Exception {
        x1 = 50;  x2 = 950; p1 = p2 = s1 = s2 = ediff = 0;turns = 10;
        mv1 = mv2 = 100;
        vb = vb1 = vb2 = 1;
        generateGround();
        isEntered = false;
        player1 = player2 = "";
        ReadHighScore();
        t.beginRendering(200, 150);
        t.setColor(Color.BLUE);
        t.draw("1- SIGLE PLAYER", 55, 130);
        t.draw("2- MULTIPLAYER", 55, 110);
        t.draw("3- LAST SCORES", 55, 90);
        t.draw("4- controls", 55, 70);
        t.draw("5- QUIT", 55, 50);
        t.setColor(Color.GRAY);
        t.endRendering();

        if (isKeyPressed(KeyEvent.VK_1)) {
            ai_or_2 = false;
            state = Phase.StartingGame;
        } else if (isKeyPressed(KeyEvent.VK_2)) {
            ai_or_2 = true;
            state = Phase.StartingGame;
            t.setColor(Color.WHITE);
        } else if (isKeyPressed(KeyEvent.VK_3)) {
            state = Phase.HighScore;
        } else if (isKeyPressed(KeyEvent.VK_4)) {
            state = Phase.Help;
        } else if (isKeyPressed(KeyEvent.VK_5)) {
            System.exit(0);
        }

    }

    public void drawHighScore() {
        t.beginRendering(300, 170);
        t.setColor(Color.BLUE);
        for (int i = 0; i < s.size(); i++) {
            t.draw((String) s.get(i), 50, (160 - ((i + 1) * 18)));
        }
        t.draw("Press Escape to return to main menu", 0, 20);
        if (isKeyPressed(KeyEvent.VK_ESCAPE)) {
            state = Phase.MainMenu;
        }
        t.setColor(Color.GRAY);
        t.endRendering();
    }

    public void drawPause() {
        t.beginRendering(200, 150);
        t.setColor(Color.BLUE);
        t.draw("Continue press Escape", 50, 100);
        t.draw("Quit press Enter ", 53, 70);
        t.setColor(Color.GRAY);
        /*if(isKeyPressed(KeyEvent.VK_ESCAPE)){
         t.setColor(Color.WHITE);
         state = Phase.Player1;
         }
         else*/ if (isKeyPressed(KeyEvent.VK_ENTER)) {
            state = Phase.MainMenu;
        }
        t.endRendering();
    }

    public void drawPlayer() {
        if (!ai_or_2) {
            player2 = "CPU\n";
            t.beginRendering(200, 150);
            t.setColor(Color.BLUE);
            t.draw("enter your name : ", 30, 90);
            t.draw(player1, 50, 70);
            if (player1.length() > 0) {
                if (player1.charAt(player1.length() - 1) == '\n') {
                    isEntered = true;
                    state = Phase.AIlevel;
                    player1 = player1.substring(0, player1.length()-1);
                }
            }
        } else {
            t.beginRendering(200, 150);
            t.setColor(Color.BLUE);
            t.draw("enter player 1 name : ", 50, 120);
            t.draw(player1, 50, 100);
            t.draw("enter player 2 name : ", 50, 60);
            t.draw(player2, 50, 40);
            if (player1.length() > 0) {
                if (player1.charAt(player1.length() - 1) == '\n') {
                    isEntered = true;
                    if (player2.length() > 0) {
                        if (player2.charAt(player2.length() - 1) == '\n') {
                            t.setColor(Color.WHITE);
                            t.endRendering();
                            state = Phase.Player1;
                            player1 = player1.substring(0, player1.length()-1);
                            player2 = player2.substring(0, player2.length()-1);
                            return;
                        }
                    }
                }
            }
        }
        t.setColor(Color.GRAY);
        t.endRendering();
    }

    public void drawAI() {
        t.beginRendering(200, 150);
        t.setColor(Color.BLUE);
        t.draw("set enemy diffculty 0 - 9 ", 30, 70);
        if (ediff != 0) {
            ediff -= 38;
            System.out.println(ediff);
            t.setColor(Color.WHITE);
            t.endRendering();
            state = Phase.Player1;
            return;
        }
        t.setColor(Color.GRAY);
        t.endRendering();
    }
    
    public void drawEnd() throws Exception{
        t.beginRendering(200, 150);
        t.setColor(Color.BLUE);
        if(s1>s2){
            t.draw("Winner : "+player1+" :"+s1, 30, 70);
            t.draw(player2+" :"+s2, 50, 50);
            if(!isPrinted)s.add( 0, player1+"    "+s1);
        }
        else if(s1 == s2 ){
            t.draw("draw !", 40, 100);
            t.draw(player1+" :"+s1, 30, 70);
            t.draw(player2+" :"+s2, 30, 50);
        }
            
        else {
            t.draw("Winner : "+player2+" :"+s2, 30, 60);
            t.draw(player1+" :"+s1, 50, 40);
            if(ai_or_2 && !isPrinted)s.add( 0, player2+"    "+s2);
        }
        if(!isPrinted){
            WriteHighScore();
            isPrinted = true;
        }
        t.draw("press Esc to Main menu", 30, 20);
        if(isKeyPressed(KeyEvent.VK_ESCAPE))state = Phase.MainMenu;
        t.setColor(Color.GRAY);
        t.endRendering();
    }
    void drawHelp(){
        t.beginRendering(300, 300);
        t.setColor(Color.BLUE);
        t.draw("up and down to control power", 60, 200);
        t.draw("A and S to move tank left or right ", 60, 170);
        t.draw("the gun moves with mouse cursor", 60, 140);
        t.draw("press space to fire" , 60 , 110);
        t.draw("press Esc to go to main menu", 60, 70);
        t.setColor(Color.GRAY);
        t.endRendering();
        if(isKeyPressed(KeyEvent.VK_ESCAPE))state = Phase.MainMenu;
    }
}