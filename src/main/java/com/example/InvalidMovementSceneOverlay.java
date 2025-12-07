package com.example;

import com.google.inject.Inject;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import net.runelite.api.Perspective;

import java.awt.Graphics2D;
import java.awt.Dimension;

import net.runelite.api.NPC;
import net.runelite.api.Player;


import java.awt.Polygon;


class InvalidMovementSceneOverlay extends Overlay
{
    public static class Vec
    {
        public final double x;
        public final double y;

        public Vec(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        public Vec sub(Vec o)
        {
            return new Vec(x - o.x, y - o.y);
        }

        public double dot(Vec o)
        {
            return x * o.x + y * o.y;
        }

        public Vec norm()
        {
            double len = Math.sqrt(x * x + y * y);
            return new Vec(x / len, y / len);
        }
    }
    private static final int LOCAL_TILE_SIZE = Perspective.LOCAL_TILE_SIZE;

    private final Client client;
    private final ExampleConfig config;

    @Inject
    public InvalidMovementSceneOverlay(Client client, ExampleConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    private void drawNpcLine(Graphics2D g, NPC npc)
    {
        if (npc == null)
        {
            return;
        }

        LocalPoint lp = npc.getLocalLocation();
        if (lp == null)
        {
            return;
        }

        // Scene projection
        Point scenePoint = Perspective.localToCanvas(
                client,
                lp,
                client.getPlane()
        );

        // Minimap projection
        Point minimapPoint = Perspective.localToMinimap(
                client,
                lp
        );

        if (scenePoint != null && minimapPoint != null)
        {
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2));
            g.drawLine(
                    scenePoint.getX(),
                    scenePoint.getY(),
                    minimapPoint.getX(),
                    minimapPoint.getY()
            );
        }
    }
    private Tile getTileUnderMouse(Client client)
    {



        Tile t = client.getSelectedSceneTile();
        if (t == null)
        {
            return null;
        }

        // Fix the 1-tile SW offset in your RL version
        int x = t.getWorldLocation().getX() + 0;
        int y = t.getWorldLocation().getY() + 0;

        WorldPoint corrected = new WorldPoint(x, y, client.getPlane());
        LocalPoint lp = LocalPoint.fromWorld(client, corrected);

        if (lp == null)
        {
            return null;
        }

        int sx = lp.getSceneX();
        int sy = lp.getSceneY();

        return client.getScene().getTiles()[client.getPlane()][sx][sy];
    }

    private void drawPlayerLine(Graphics2D g, Player player)
    {
        if (player == null)
        {
            return;
        }

        LocalPoint lp = player.getLocalLocation();
        if (lp == null)
        {
            return;
        }

        // Scene projection
        Point scenePoint = Perspective.localToCanvas(
                client,
                lp,
                client.getPlane()
        );

        // Minimap projection
        Point minimapPoint = Perspective.localToMinimap(
                client,
                lp
        );

        if (scenePoint != null && minimapPoint != null)
        {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawLine(
                    scenePoint.getX(),
                    scenePoint.getY(),
                    minimapPoint.getX(),
                    minimapPoint.getY()
            );
        }
    }

    public static Polygon computeMinimumBoundingRectangle(Point... pts)
    {
        // Convert input points to Vec
        Vec[] v = new Vec[pts.length];
        for (int i = 0; i < pts.length; i++)
        {
            v[i] = new Vec(pts[i].getX(), pts[i].getY());
        }

        double bestArea = Double.MAX_VALUE;

        Vec bestU = null;
        Vec bestV = null;

        double bestMinU = 0, bestMaxU = 0;
        double bestMinV = 0, bestMaxV = 0;

        // Rotating calipers: try each edge orientation
        for (int i = 0; i < v.length; i++)
        {
            Vec p = v[i];
            Vec q = v[(i + 1) % v.length];

            // Axis U = edge direction
            Vec U = q.sub(p).norm();

            // Axis V = perpendicular to U
            Vec V = new Vec(-U.y, U.x);

            double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
            double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;

            // Project all points onto U and V
            for (Vec pt : v)
            {
                double projU = pt.dot(U);
                double projV = pt.dot(V);

                minU = Math.min(minU, projU);
                maxU = Math.max(maxU, projU);
                minV = Math.min(minV, projV);
                maxV = Math.max(maxV, projV);
            }

            double area = (maxU - minU) * (maxV - minV);

            if (area < bestArea)
            {
                bestArea = area;
                bestU = U;
                bestV = V;
                bestMinU = minU;
                bestMaxU = maxU;
                bestMinV = minV;
                bestMaxV = maxV;
            }
        }

        // Convert the best rectangle back into screen-space coordinates
        Vec c1 = new Vec(
                bestU.x * bestMinU + bestV.x * bestMinV,
                bestU.y * bestMinU + bestV.y * bestMinV
        );

        Vec c2 = new Vec(
                bestU.x * bestMaxU + bestV.x * bestMinV,
                bestU.y * bestMaxU + bestV.y * bestMinV
        );

        Vec c3 = new Vec(
                bestU.x * bestMaxU + bestV.x * bestMaxV,
                bestU.y * bestMaxU + bestV.y * bestMaxV
        );

        Vec c4 = new Vec(
                bestU.x * bestMinU + bestV.x * bestMaxV,
                bestU.y * bestMinU + bestV.y * bestMaxV
        );

        // Build polygon
        Polygon poly = new Polygon();
        poly.addPoint((int) c1.x, (int) c1.y);
        poly.addPoint((int) c2.x, (int) c2.y);
        poly.addPoint((int) c3.x, (int) c3.y);
        poly.addPoint((int) c4.x, (int) c4.y);

        return poly;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {


        if (client == null)
        {
            System.out.println("Client is null");
            return null;
        }
        for (Player p : client.getPlayers())
        {
            if (p != null && p != client.getLocalPlayer())
            {
                drawPlayerLine(graphics, p);
            }
        }
        for (NPC npc : client.getNpcs())
        {
            if (npc.getName() != null )
            {
                drawNpcLine(graphics, npc);
            }
        }




       Tile tile = getTileUnderMouse(client);
        if (tile == null)
        {
            System.out.println("Tile is null");
            return null;
        }
   LocalPoint lp = tile.getLocalLocation();
        if (lp == null)
        {
            System.out.println("localPoint is null");
            return null;
        }
//begin bounding box
        int plane = client.getPlane();
        int x = lp.getX();
        int y = lp.getY();

        // Corners in local space (assuming lp is roughly center, this is "tile-ish"; we mainly care about getting 4 distinct points)
        LocalPoint sw = new LocalPoint(x - 64, y - 64);
        LocalPoint se = new LocalPoint(x + 64, y - 64);
        LocalPoint nw = new LocalPoint(x - 64, y + 64);
        LocalPoint ne = new LocalPoint(x + 64, y + 64);

        Point swP = Perspective.localToCanvas(client, sw, plane);
        Point seP = Perspective.localToCanvas(client, se, plane);
        Point nwP = Perspective.localToCanvas(client, nw, plane);
        Point neP = Perspective.localToCanvas(client, ne, plane);

        if (swP == null || seP == null || nwP == null || neP == null)
        {
            return null;
        }

        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();

//        // Southwest – red
//        graphics.setColor(Color.RED);
//        graphics.drawLine(swP.getX(), 0, swP.getX(), canvasHeight);           // vertical at swP.x
//        graphics.drawLine(0, swP.getY(), canvasWidth, swP.getY());            // horizontal at swP.y
//
//        // Southeast – green
//        graphics.setColor(Color.GREEN);
//        graphics.drawLine(seP.getX(), 0, seP.getX(), canvasHeight);
//        graphics.drawLine(0, seP.getY(), canvasWidth, seP.getY());
//
//        // Northwest – blue
//        graphics.setColor(Color.BLUE);
//        graphics.drawLine(nwP.getX(), 0, nwP.getX(), canvasHeight);
//        graphics.drawLine(0, nwP.getY(), canvasWidth, nwP.getY());
//
//        // Northeast – magenta
//        graphics.setColor(Color.MAGENTA);
//        graphics.drawLine(neP.getX(), 0, neP.getX(), canvasHeight);
//        graphics.drawLine(0, neP.getY(), canvasWidth, neP.getY());

        int minX = Math.min(Math.min(swP.getX(), seP.getX()),
                Math.min(nwP.getX(), neP.getX()));

        int maxX = Math.max(Math.max(swP.getX(), seP.getX()),
                Math.max(nwP.getX(), neP.getX()));

        int minY = Math.min(Math.min(swP.getY(), seP.getY()),
                Math.min(nwP.getY(), neP.getY()));

        int maxY = Math.max(Math.max(swP.getY(), seP.getY()),
                Math.max(nwP.getY(), neP.getY()));

        Rectangle rect = new Rectangle(
                minX,
                minY,
                maxX - minX,
                maxY - minY
        );

        graphics.setColor(Color.WHITE);
        graphics.draw(rect);


        //end bounding box

//start obb



        Polygon obb = computeMinimumBoundingRectangle(swP, seP, neP, nwP);

        graphics.setColor(Color.CYAN);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawPolygon(obb);


//end obb
        // Scene projection
        Point scenePoint = Perspective.localToCanvas(client, lp, client.getPlane());

        // Minimap projection
        Point minimapPoint = Perspective.localToMinimap(client, lp);

        if (scenePoint != null && minimapPoint != null)
        {
            graphics.setColor(Color.CYAN);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawLine(scenePoint.getX(), scenePoint.getY(),
                    minimapPoint.getX(), minimapPoint.getY());
        }


        if (config.showScene())
        {
            renderScene(graphics);
        }
        return null;
    }

    private void renderScene(Graphics2D graphics)
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        final LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        final int playerX = playerLocation.getSceneX();
        final int playerY = playerLocation.getSceneY();
        final int radius = config.radiusScene() < 0 ? Integer.MAX_VALUE / 2 : config.radiusScene();

        WorldView worldView = client.getTopLevelWorldView();

        CollisionData[] collisionData = worldView.getCollisionMaps();

        if (collisionData == null)
        {
            return;
        }

        final BasicStroke borderStroke = new BasicStroke((float) config.wallWidth());

        final int z = worldView.getPlane();

        final int[][] flags = collisionData[z].getFlags();

        final Tile[][] tiles = worldView.getScene().getTiles()[z];

        final int startX = Math.max(playerX - radius, 0);
        final int endX = Math.min(playerX + radius, tiles[0].length);
        final int startY = Math.max(playerY - radius, 0);
        final int endY = Math.min(playerY + radius, tiles.length);

        for (int y = startY; y < endY; y++)
        {
            for (int x = startX; x < endX; x++)
            {
                Tile tile = tiles[x][y];
                if (tile == null)
                {
                    continue;
                }

                final LocalPoint localPoint = tile.getLocalLocation();
                if (localPoint == null)
                {
                    continue;
                }

                final Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
                if (poly == null)
                {
                    continue;
                }

                final int data = flags[tile.getSceneLocation().getX()][tile.getSceneLocation().getY()];

                final Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

                graphics.setStroke(borderStroke);

                if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR))
                {
                    graphics.setColor(config.colourFloor());
                    graphics.fill(poly);
                }

                if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_OBJECT))
                {
                    graphics.setColor(config.colourObject());
                    graphics.fill(poly);
                }

                if (tile.getWallObject() != null)
                {
                    final GeneralPath path = new GeneralPath();

                    graphics.setColor(config.colourWall());

                    if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH))
                    {
                        drawWall(path, localPoint.getX(), localPoint.getY(), z, LOCAL_TILE_SIZE, 0);
                    }
                    if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST))
                    {
                        drawWall(path, localPoint.getX(), localPoint.getY(), z, 0, LOCAL_TILE_SIZE);
                    }
                    if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH))
                    {
                        drawWall(path, localPoint.getX(), localPoint.getY() + LOCAL_TILE_SIZE, z, LOCAL_TILE_SIZE, 0);
                    }
                    if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST))
                    {
                        drawWall(path, localPoint.getX() + LOCAL_TILE_SIZE, localPoint.getY(), z, 0, LOCAL_TILE_SIZE);
                    }

                    graphics.draw(path);
                }
            }
        }
    }

    private void drawWall(final GeneralPath path, int x, int y, int z, int dx, int dy)
    {
        final boolean hasFirst = moveTo(path, x, y, z);

        x += dx;
        y += dy;

        if (hasFirst)
        {
            lineTo(path, x, y, z);
        }
    }

    private boolean moveTo(final GeneralPath path, final int x, final int y, final int z)
    {
        Point point = XYToPoint(x, y, z);
        if (point != null)
        {
            path.moveTo(point.getX(), point.getY());
            return true;
        }
        return false;
    }

    private void lineTo(final GeneralPath path, final int x, final int y, final int z)
    {
        Point point = XYToPoint(x, y, z);
        if (point != null)
        {
            path.lineTo(point.getX(), point.getY());
        }
    }

    private Point XYToPoint(final int x, final int y, final int z)
    {
        return Perspective.localToCanvas(
                client,
                new LocalPoint(x - LOCAL_TILE_SIZE / 2, y - LOCAL_TILE_SIZE / 2),
                z);
    }
}