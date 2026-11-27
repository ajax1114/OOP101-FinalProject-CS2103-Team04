package com.game.dungeon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Mga Nilalang — five-stage Filipino folklore dungeon.
 * Uses the supplied map.json, tile atlas, player strips and enemy GIF frame exports.
 */
public class DungeonGame extends JPanel implements ActionListener, KeyListener {
    private static final int TILE = 16, VIEW_W = 960, VIEW_H = 640;
    private static final String[] TYPES = {"Tiyanak", "Aswang", "Tikbalang", "Manananggal", "Kapre"};
    private static final String[] KEYS = {"tiyanak", "aswang", "tikbalang", "manananggal", "kapre"};
    private static final Rectangle[] AREAS = {
        new Rectangle(10, 7, 20, 15),   // smallest starting chamber
        new Rectangle(25, 4, 27, 17),   // upper hall
        new Rectangle(21, 19, 22, 15),  // west middle chamber
        new Rectangle(34, 19, 35, 12),  // east middle chamber
        new Rectangle(20, 30, 49, 16)   // large lower chamber
    };
    private static final int[][] SPAWNS = {{15,15},{33,15},{28,25},{50,24},{34,38}};
    private static final int[][][] ENEMY_SPAWNS = {
        {{12,11},{17,12},{17,18}},
        {{27,9},{35,15},{40,10},{48,12}},
        {{24,24},{28,23},{30,24}},
        {{39,24},{45,22},{48,25},{64,23}},
        {{25,36},{34,40},{39,35},{54,40},{63,36}}
    };

    private BufferedImage atlas;
    private int mapWidth, mapHeight;
    private final List<Tile> drawTiles = new ArrayList<>();
    private boolean[][] wall;
    private boolean[][] floor;
    private final Set<Long> trapTiles = new HashSet<>();
    private int trapDamageCooldown = 0;
    private BufferedImage[] doorFrames;
    private final List<Door> doors = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    // Enemies defeated in one stage return as hostile reinforcements in the next.
    private final List<String> pendingEnemyReinforcements = new ArrayList<>();
    private final List<String> defeatedThisLevel = new ArrayList<>();
    private final Map<String, BufferedImage[]> playerFrames = new HashMap<>();
    private final javax.swing.Timer timer;
    private final Random random = new Random(412);

    private float px, py, playerAnimTime, playerAttackTime;
    private int facing = 3; // 0 up, 1 down, 2 left, 3 right
    private boolean up, down, left, right, attackPressed, interactPressed;
    private float playerHp = 100;
    private int level = 0, kills, reinforcementsInCurrentLevel;
    private float cameraX, cameraY, zoom = 1.0f;
    private int playerHurtFrames, attackCooldown;
    private float stageBanner = 2.0f;
    private boolean gameWon, gameOver;
    private final Font ui = new Font("Dialog", Font.BOLD, 15);

    private static class Tile { int id,x,y; String layer; Tile(int id,int x,int y,String layer){this.id=id;this.x=x;this.y=y;this.layer=layer;} }
    private static class Door {
        float x,y,w,h,open=0; boolean opening=false;
        Door(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;}
        Rectangle bounds(){return new Rectangle((int)x,(int)y,(int)w,(int)h);}
    }
    private static class Enemy {
        String type; float x,y,hp,maxHp; float animTime=0; int attackCooldown=0,hitFlash=0;
        boolean dying=false,dead=false; float deathTime=0; boolean facingLeft=true;
        BufferedImage[] walkR,walkL,attackR,attackL,die;
        String action="walk";
        boolean alerted=false;
        float alertTime=0;
        Enemy(String type,float x,float y){this.type=type;this.x=x;this.y=y;maxHp=hp=type.equals("kapre")?145:85;}
    }

    public DungeonGame() throws Exception {
        setPreferredSize(new Dimension(VIEW_W,VIEW_H)); setFocusable(true); setBackground(Color.BLACK); addKeyListener(this);
        addMouseWheelListener(e -> { setZoom(zoom * (e.getWheelRotation() < 0 ? 1.12f : 1f / 1.12f)); repaint(); });
        atlas=ImageIO.read(new File("assets/map/tiles.png"));
        String json=Files.readString(Path.of("assets/map/map.json"));
        mapWidth=readNumber(json,"mapWidth"); mapHeight=readNumber(json,"mapHeight");
        wall=new boolean[mapWidth][mapHeight];
        floor=new boolean[mapWidth][mapHeight];
        List<List<Tile>> parsedLayers=new ArrayList<>(); List<String> layerNames=new ArrayList<>();
        Pattern lp=Pattern.compile("\\{\"name\":\"([^\"]*)\",\"tiles\":\\[(.*?)\\],\"collider\":(?:true|false)\\}",Pattern.DOTALL);
        Matcher lm=lp.matcher(json); Pattern tp=Pattern.compile("\\{\"id\":\"(\\d+)\",\"x\":(\\d+),\"y\":(\\d+)\\}");
        while(lm.find()){
            String name=lm.group(1); String tileText=lm.group(2); List<Tile> layerTiles=new ArrayList<>(); Matcher tm=tp.matcher(tileText);
            while(tm.find()){
                int id=Integer.parseInt(tm.group(1)),x=Integer.parseInt(tm.group(2)),y=Integer.parseInt(tm.group(3));
                Tile tile=new Tile(id,x,y,name); layerTiles.add(tile);
                if(x>=0&&y>=0&&x<mapWidth&&y<mapHeight){
                    if(isSolidLayer(name))wall[x][y]=true;
                    if(name.trim().equalsIgnoreCase("traps"))trapTiles.add((((long)y)<<32)|(x&0xffffffffL));
                    // The base floor layer is the authoritative walkable mask. Props and
                    // decorative floor details never make an empty tile walkable.
                    if(name.equalsIgnoreCase("New layer") && (id==0 || id==1))floor[x][y]=true;
                }
            }
            layerNames.add(name); parsedLayers.add(layerTiles);
        }
        // JSON layers are stored front-to-back; reverse order paints the floor first and wall/props above it.
        for(int li=parsedLayers.size()-1;li>=0;li--)drawTiles.addAll(parsedLayers.get(li));
        if(parsedLayers.isEmpty())throw new IOException("No map layers were parsed from assets/map/map.json");
        // Read exact door anchors from the supplied map's "door wall open" layer.
        Matcher doorLayer = Pattern.compile("\\\"name\\\":\\\"door wall open\\\".*?\\\"tiles\\\":\\[(.*?)\\]", Pattern.DOTALL).matcher(json);
        if (doorLayer.find()) {
            Matcher anchor = Pattern.compile("\\{\\\"id\\\":\\\"78\\\",\\\"x\\\":(\\d+),\\\"y\\\":(\\d+)\\}").matcher(doorLayer.group(1));
            while (anchor.find()) {
                int tx=Integer.parseInt(anchor.group(1)), ty=Integer.parseInt(anchor.group(2));
                doors.add(new Door(tx*TILE,ty*TILE,2*TILE,2*TILE));
            }
        }
        if (doors.isEmpty()) throw new IOException("No door anchors found in the map door layer.");
        // The original map's door frames share tile cells with wall layers. Remove only the
        // door leaf cells from static collision; the animated Door object blocks that space.
        for(Door d:doors){
            int x0=(int)(d.x/TILE), y0=(int)(d.y/TILE);
            int doorHeightTiles=(int)Math.ceil(d.h/TILE);
            for(int x=x0;x<x0+2;x++)for(int y=y0;y<y0+doorHeightTiles;y++)if(x>=0&&y>=0&&x<mapWidth&&y<mapHeight)wall[x][y]=false;
        }
        doorFrames=strip("assets/map/DoorAnimation.png",9,95,113);
        loadPlayer();
        beginLevel(0);
        timer=new javax.swing.Timer(16,this); timer.start();
    }

    private int readNumber(String json,String key)throws IOException{Matcher m=Pattern.compile("\""+key+"\"\\s*:\\s*(\\d+)").matcher(json);if(!m.find())throw new IOException("Missing "+key+" in map.json");return Integer.parseInt(m.group(1));}
    private boolean isSolidLayer(String n){
        String s=n.toLowerCase(Locale.ROOT);
        // Only structural wall layers block movement. The general "details" layer
        // contains decorative floor overlays; treating every one of those tiles as
        // a full-tile wall creates invisible barriers across otherwise open rooms.
        return s.equals("wall vertical") || s.equals("wall horizontal") ||
               s.trim().equals("walls details") || s.trim().equals("details wall/ doors");
    }
    private BufferedImage[] strip(String path,int frames,int cw,int ch) throws Exception {
        BufferedImage src=ImageIO.read(new File(path)); BufferedImage[] out=new BufferedImage[frames];
        for(int i=0;i<frames;i++){
            int x=i*cw; if(x+cw>src.getWidth()) throw new IOException("Sprite strip too short: "+path);
            out[i]=src.getSubimage(x,0,cw,Math.min(ch,src.getHeight()));
        }
        return out;
    }
    private void loadPlayer() throws Exception {
        playerFrames.put("down",strip("assets/player/walk_down.png",8,100,150));
        playerFrames.put("up",strip("assets/player/walk_up.png",7,48,62));
        playerFrames.put("left",strip("assets/player/walk_left.png",8,48,62));
        playerFrames.put("right",strip("assets/player/walk_right.png",9,50,64));
        playerFrames.put("swordLeft",strip("assets/player/sword_left.png",6,150,160));
        playerFrames.put("swordRight",strip("assets/player/sword_right.png",6,160,150));
    }
    private BufferedImage[] frames(String type,String state){
        File dir=new File("assets/enemies/"+type+"/"+state); File[] files=dir.listFiles((d,n)->n.endsWith(".png"));
        if(files==null||files.length==0)return new BufferedImage[0]; Arrays.sort(files); List<BufferedImage> a=new ArrayList<>();
        for(File f:files)try{a.add(ImageIO.read(f));}catch(Exception ignored){}
        return a.toArray(new BufferedImage[0]);
    }
    private void loadEnemyAnimations(Enemy e){
        String t=e.type;
        e.walkR=frames(t,"walkR"); e.walkL=frames(t,"walkL"); e.attackR=frames(t,"attackR"); e.attackL=frames(t,"attackL"); e.die=frames(t,"die");
        // Where the source pack has only one direction, mirror its original animation rather than substituting art.
        if(e.walkL.length==0)e.walkL=e.walkR;
        if(e.attackL.length==0)e.attackL=e.attackR;
    }
    private void beginLevel(int n){
        level=n; enemies.clear(); defeatedThisLevel.clear();
        for(Door d:doors){d.open=0;d.opening=false;}
        int[] playerStart=nearestWalkable(SPAWNS[level][0],SPAWNS[level][1]);
        px=playerStart[0]*TILE+8; py=playerStart[1]*TILE+8;
        // Native enemies for this level.
        for(int[] p:ENEMY_SPAWNS[level]) spawnEnemy(KEYS[level],p[0],p[1],false);
        // Previous-level kills return as hostile enemies beside the new level's enemies.
        reinforcementsInCurrentLevel=pendingEnemyReinforcements.size();
        for(String type:pendingEnemyReinforcements) spawnEnemy(type,-1,-1,true);
        pendingEnemyReinforcements.clear();
        stageBanner=2.3f; gameOver=false; updateCamera(true);
    }

    private void spawnEnemy(String type,int preferredX,int preferredY,boolean reinforcement){
        int sx=-1,sy=-1;
        if(!reinforcement){int[] safe=nearestWalkable(preferredX,preferredY);sx=safe[0];sy=safe[1];}
        else {
            Rectangle area=AREAS[level];
            int minX=Math.max(0,area.x),maxX=Math.min(mapWidth-1,area.x+area.width);
            int minY=Math.max(0,area.y),maxY=Math.min(mapHeight-1,area.y+area.height);
            for(int attempt=0;attempt<700;attempt++){
                int x=minX+random.nextInt(Math.max(1,maxX-minX+1));
                int y=minY+random.nextInt(Math.max(1,maxY-minY+1));
                float wx=x*TILE+8,wy=y*TILE+8;
                if(!canStand(wx,wy)||Math.hypot(wx-px,wy-py)<100)continue;
                boolean crowded=false;
                for(Enemy other:enemies)if(Math.hypot(wx-other.x,wy-other.y)<48){crowded=true;break;}
                if(!crowded){sx=x;sy=y;break;}
            }
            if(sx<0){int[] safe=nearestWalkable(SPAWNS[level][0]+3,SPAWNS[level][1]+3);sx=safe[0];sy=safe[1];}
        }
        Enemy enemy=new Enemy(type,sx*TILE+8,sy*TILE+8);
        loadEnemyAnimations(enemy);enemies.add(enemy);
    }
    private boolean inDoor(float x,float y){
        for(Door d:doors)if(d.open<0.96f && x+6>d.x&&x-6<d.x+d.w&&y+3>d.y&&y-5<d.y+d.h)return true;
        return false;
    }
    private boolean canStand(float x,float y){
        // Collision is checked at the character's feet-center tile. The old five-point
        // footprint rejected movement whenever a corner touched an unpainted/overlay
        // tile, which made narrow corridors and decorated floor areas impassable.
        // Structural wall layers remain solid; floor details and traps remain walkable.
        int tx=(int)Math.floor(x/TILE), ty=(int)Math.floor(y/TILE);
        if(tx<0||ty<0||tx>=mapWidth||ty>=mapHeight)return false;
        if(!floor[tx][ty]||wall[tx][ty])return false;
        return !inDoor(x,y);
    }
    private void setZoom(float value){zoom=Math.max(0.65f,Math.min(2.25f,value));updateCamera(false);}
    private void updateCamera(boolean snap){
        float visibleW=VIEW_W/zoom,visibleH=VIEW_H/zoom;
        float targetX=px-visibleW/2f,targetY=py-visibleH/2f;
        targetX=Math.max(0,Math.min(mapWidth*TILE-visibleW,targetX));
        targetY=Math.max(0,Math.min(mapHeight*TILE-visibleH,targetY));
        if(snap){cameraX=targetX;cameraY=targetY;}
        else {cameraX+=(targetX-cameraX)*0.22f;cameraY+=(targetY-cameraY)*0.22f;}
    }
    private boolean inCurrentArea(float x,float y){Rectangle r=AREAS[level];return x>=r.x*TILE&&x<=(r.x+r.width)*TILE&&y>=r.y*TILE&&y<=(r.y+r.height)*TILE;}
    private int[] nearestWalkable(int tx,int ty){
        Rectangle area=AREAS[level];int bestX=tx,bestY=ty;double best=Double.MAX_VALUE;
        int minX=Math.max(0,area.x),maxX=Math.min(mapWidth-1,area.x+area.width);
        int minY=Math.max(0,area.y),maxY=Math.min(mapHeight-1,area.y+area.height);
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
            float cx=x*TILE+8,cy=y*TILE+8;
            if(canStand(cx,cy)){double d=(x-tx)*(double)(x-tx)+(y-ty)*(double)(y-ty);if(d<best){best=d;bestX=x;bestY=y;}}
        }
        if(best==Double.MAX_VALUE)throw new IllegalStateException("No walkable floor tile in level area "+(level+1));
        return new int[]{bestX,bestY};
    }

    private void updateGame(){
        if(gameWon||gameOver)return;
        float dt=0.016f;
        if(stageBanner>0)stageBanner-=dt;
        if(playerHurtFrames>0)playerHurtFrames--; if(attackCooldown>0)attackCooldown--;
        if(playerAttackTime>0)playerAttackTime=Math.max(0,playerAttackTime-dt);
        float dx=(right?1:0)-(left?1:0),dy=(down?1:0)-(up?1:0); float len=(float)Math.hypot(dx,dy);
        if(len>0){dx/=len;dy/=len; if(Math.abs(dx)>Math.abs(dy))facing=dx<0?2:3;else facing=dy<0?0:1; playerAnimTime+=dt;}
        float speed=2.35f;
        if(canStand(px+dx*speed,py))px+=dx*speed;
        if(canStand(px,py+dy*speed))py+=dy*speed;
        if(trapDamageCooldown>0)trapDamageCooldown--;
        int playerTileX=(int)Math.floor(px/TILE), playerTileY=(int)Math.floor(py/TILE);
        long playerTileKey=(((long)playerTileY)<<32)|(playerTileX&0xffffffffL);
        if(trapTiles.contains(playerTileKey) && trapDamageCooldown<=0){
            playerHp=Math.max(0,playerHp-8); trapDamageCooldown=48; playerHurtFrames=12;
            if(playerHp<=0)gameOver=true;
        }
        if(attackPressed&&attackCooldown==0){
            attackCooldown=28; playerAttackTime=0.42f;
            for(Enemy e:enemies)if(!e.dead&&!e.dying&&Math.hypot(e.x-px,e.y-py)<57){e.hp-=42;e.hitFlash=7;if(e.hp<=0){e.dying=true;e.deathTime=0;e.action="die";defeatedThisLevel.add(e.type);}}
        }
        if(interactPressed){Door nearest=null;double best=64;
            for(Door d:doors){double dist=Math.hypot(d.x+d.w/2-px,d.y+d.h/2-py);if(dist<best&&d.open<1){best=dist;nearest=d;}}
            if(nearest!=null)nearest.opening=true; interactPressed=false;
        }
        for(Door d:doors)if(d.opening)d.open=Math.min(1,d.open+dt/0.78f);
        // Hostile enemies provide a visible preparation window before chasing or attacking.
        for(Enemy e:enemies){
            if(e.dead)continue; e.animTime+=dt;
            if(e.hitFlash>0)e.hitFlash--;
            if(e.dying){e.deathTime+=dt;if(e.die.length==0||e.deathTime>Math.max(0.65f,e.die.length*0.075f)){e.dead=true;}continue;}
            double dist=Math.hypot(px-e.x,py-e.y);
            if(!e.alerted && dist<190){e.alerted=true;e.alertTime=2.0f;}
            if(e.alerted && e.alertTime>0){
                e.alertTime=Math.max(0,e.alertTime-dt);
                e.action="walk"; // alert / ready stance: no damage during the warning period
                e.facingLeft=px<e.x;
            } else if(e.alerted && dist<230){
                e.facingLeft=px<e.x;
                if(dist>34){e.action="walk";float vx=(px-e.x)/(float)dist,vy=(py-e.y)/(float)dist;float nx=e.x+vx*0.72f,ny=e.y+vy*0.72f;
                    if(canStand(nx,ny)){e.x=nx;e.y=ny;}}
                else {e.action="attack";if(e.attackCooldown<=0){playerHp-=e.type.equals("kapre")?13:9;e.attackCooldown=58;playerHurtFrames=12;if(playerHp<=0){playerHp=0;gameOver=true;}}}
            } else e.action="walk";
            if(e.attackCooldown>0)e.attackCooldown--;
        }
        enemies.removeIf(e->e.dead);
        if(enemies.isEmpty()){
            if(level<TYPES.length-1){
                pendingEnemyReinforcements.addAll(defeatedThisLevel);
                beginLevel(level+1);
            } else {gameWon=true;}
        }
        updateCamera(false);
    }
    @Override public void actionPerformed(ActionEvent e){updateGame();repaint();}

    private void drawMirrored(Graphics2D g,BufferedImage im,int x,int y,int w,int h,boolean flip){
        if(im==null)return; if(!flip){g.drawImage(im,x,y,w,h,null);}else{g.drawImage(im,x+w,y,x,y+h,0,0,im.getWidth(),im.getHeight(),null);}
    }
    private BufferedImage frame(BufferedImage[] a,float time,float fps){return a.length==0?null:a[(int)(time*fps)%a.length];}
    private void drawDoor(Graphics2D g,Door d){
        if(doorFrames==null||doorFrames.length==0)return;
        int index=Math.min(doorFrames.length-1,(int)(d.open*(doorFrames.length-1)+0.0001f));
        BufferedImage frame=doorFrames[index];
        int w=48,h=57;
        // The supplied strip includes its own stone arch and hinged wooden leaf.
        // The map anchor (tile 78) is the upper-left jamb of the doorway.
        // Place the sprite from that anchor instead of centering it in the
        // collision rectangle; centering shifted the hinged leaf off the arch.
        g.drawImage(frame,(int)d.x,(int)d.y,w,h,null);
    }
    @Override protected void paintComponent(Graphics gg){
        super.paintComponent(gg);
        Graphics2D screen=(Graphics2D)gg.create();
        screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        screen.setColor(Color.BLACK);screen.fillRect(0,0,VIEW_W,VIEW_H);
        Graphics2D world=(Graphics2D)screen.create();
        world.scale(zoom,zoom);world.translate(-cameraX,-cameraY);
        for(Tile t:drawTiles){
            int sx=(t.id%8)*TILE,sy=(t.id/8)*TILE;
            if(sx+TILE<=atlas.getWidth()&&sy+TILE<=atlas.getHeight())
                world.drawImage(atlas.getSubimage(sx,sy,TILE,TILE),t.x*TILE,t.y*TILE,null);
        }
        Rectangle a=AREAS[level];world.setColor(new Color(230,180,80,18));
        world.fillRect(a.x*TILE,a.y*TILE,a.width*TILE,a.height*TILE);
        for(Door d:doors)drawDoor(world,d);
        for(Enemy e:enemies){
            if(e.dead)continue;boolean flip=e.facingLeft;BufferedImage[] anim;
            if(e.dying)anim=e.die;else if(e.action.equals("attack"))anim=flip?e.attackL:e.attackR;else anim=flip?e.walkL:e.walkR;
            BufferedImage im=frame(anim,e.dying?e.deathTime:e.animTime,e.dying?13:11);
            int size=e.type.equals("kapre")?58:48,drawW=size,drawH=size;
            if(im!=null){float ratio=(float)im.getWidth()/Math.max(1,im.getHeight());drawW=(int)(size*Math.max(.7f,Math.min(1.35f,ratio)));}
            if(e.hitFlash>0&&!e.dying){world.setColor(new Color(255,255,255,120));world.fillOval((int)e.x-size/2,(int)e.y-size/2,size,size);}
            drawMirrored(world,im,(int)e.x-drawW/2,(int)e.y-drawH+7,drawW,drawH,flip&&(anim==e.walkR||anim==e.attackR));
            if(!e.dying){world.setColor(new Color(25,15,15));world.fillRoundRect((int)e.x-19,(int)e.y-size/2-8,38,5,3,3);world.setColor(new Color(190,45,38));world.fillRoundRect((int)e.x-19,(int)e.y-size/2-8,(int)(38*Math.max(0,e.hp/e.maxHp)),5,3,3);
                if(e.alerted&&e.alertTime>0){world.setFont(new Font("Dialog",Font.BOLD,13));world.setColor(new Color(255,220,90));world.drawString("!",(int)e.x-3,(int)e.y-size/2-14);}}
        }
        BufferedImage[] p=playerAttackTime>0?playerFrames.get(facing==2?"swordLeft":"swordRight"):
            playerFrames.get(facing==0?"up":facing==1?"down":facing==2?"left":"right");
        BufferedImage pi=frame(p,playerAnimTime,10);
        if(pi!=null){int w=playerAttackTime>0?50:(facing==1?38:34),h=playerAttackTime>0?50:(facing==1?54:46);drawMirrored(world,pi,(int)px-w/2,(int)py-h+7,w,h,false);}
        world.dispose();
        drawHud(screen);
        if(stageBanner>0){screen.setFont(new Font("Serif",Font.BOLD,30));screen.setColor(new Color(0,0,0,175));screen.fillRoundRect(320,90,320,62,14,14);screen.setColor(new Color(255,222,160));screen.drawString("LEVEL "+(level+1)+" — "+TYPES[level].toUpperCase(Locale.ROOT),340,129);}
        if(gameOver||gameWon){screen.setColor(new Color(0,0,0,210));screen.fillRect(0,0,VIEW_W,VIEW_H);screen.setColor(Color.WHITE);screen.setFont(new Font("Serif",Font.BOLD,42));String msg=gameWon?"DUNGEON CLEARED":"YOU HAVE FALLEN";screen.drawString(msg,VIEW_W/2-screen.getFontMetrics().stringWidth(msg)/2,270);screen.setFont(ui);String sub=gameWon?"You survived all five enemy stages.":"Press R to restart from Level 1.";screen.drawString(sub,VIEW_W/2-screen.getFontMetrics().stringWidth(sub)/2,315);}
        screen.dispose();
    }
    private void drawHud(Graphics2D g){
        g.setColor(new Color(12,10,12,220));g.fillRoundRect(12,10,470,92,12,12);g.setFont(ui);g.setColor(new Color(255,225,180));g.drawString("MGA NILALANG  •  LEVEL "+(level+1)+" / 5  •  "+TYPES[level],24,33);
        g.setColor(new Color(65,35,35));g.fillRoundRect(24,44,180,12,6,6);g.setColor(new Color(190,52,48));g.fillRoundRect(24,44,(int)(180*playerHp/100),12,6,6);
        g.setColor(Color.WHITE);g.drawString("HP "+(int)playerHp+"%     Enemies: "+enemies.size(),216,55);
        g.setColor(new Color(230,205,150));g.drawString("Returning hostile reinforcements: "+reinforcementsInCurrentLevel,24,82);
        g.setFont(new Font("Dialog",Font.PLAIN,12));g.setColor(new Color(245,235,215));g.drawString("WASD: Move   SPACE: Attack   E: Door   Wheel / +/-: Zoom   R: Restart",500,26);
    }
    @Override public void keyPressed(KeyEvent e){int k=e.getKeyCode();if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP)up=true;if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN)down=true;if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT)left=true;if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT)right=true;if(k==KeyEvent.VK_SPACE)attackPressed=true;if(k==KeyEvent.VK_E)interactPressed=true;if(k==KeyEvent.VK_EQUALS||k==KeyEvent.VK_ADD) setZoom(zoom*1.12f);if(k==KeyEvent.VK_MINUS||k==KeyEvent.VK_SUBTRACT) setZoom(zoom/1.12f);if(k==KeyEvent.VK_R&&(gameOver||gameWon)){pendingEnemyReinforcements.clear();defeatedThisLevel.clear();reinforcementsInCurrentLevel=0;kills=0;playerHp=100;gameWon=false;beginLevel(0);}}
    @Override public void keyReleased(KeyEvent e){int k=e.getKeyCode();if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP)up=false;if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN)down=false;if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT)left=false;if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT)right=false;if(k==KeyEvent.VK_SPACE)attackPressed=false;}
    @Override public void keyTyped(KeyEvent e){}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{try{JFrame f=new JFrame("Mga Nilalang — Filipino Folklore Dungeon");DungeonGame game=new DungeonGame();f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);f.setResizable(false);f.add(game);f.pack();f.setLocationRelativeTo(null);f.setVisible(true);game.requestFocusInWindow();}catch(Exception ex){ex.printStackTrace();JOptionPane.showMessageDialog(null,"Could not start game:\n"+ex.getMessage(),"Game error",JOptionPane.ERROR_MESSAGE);}});}
}
