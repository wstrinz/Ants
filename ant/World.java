/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ant;

import akka.actor.*;
import akka.routing.BroadcastRouter;
import akka.transactor.Coordinated;
import akka.util.Timeout;
import ant.gui.GUIRequest;
import ant.point.PointQuadTree;
import akka.actor.Props;

import java.awt.Point;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import scala.actors.threadpool.Arrays;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

//import scala.collection.immutable.TreeSet;
//import scala.math.Ordering.StringOrdering;

/**
 *
 * @author Z98
 */
public class World extends UntypedActor {
    int xdim;
    int ydim;
    
    public static PointQuadTree<ActorRef> foodPatches = null;
    public static HashMap<Point, ActorRef> patchMap = null;
    public static HashMap<ActorRef, Point> antMap = null;
    public static HashMap<ActorRef, Enter> moveList = null;
    
    private ActorRef bRouter;
    //TreeSet<ActorRef> patches;
    TreeSet<String> patchRoutes;
    TreeSet<ActorRef> patches;
    
    public static Random foodRandom = null;
    public static Random antRandom = null;
    
    public World(int xDim, int yDim)
    {
        if(foodRandom == null)
            foodRandom = new Random(System.currentTimeMillis());
        if(patchMap == null)
            patchMap = new HashMap<>();
        if(antMap == null)
        	antMap = new HashMap<>();
        if(moveList == null)
        	moveList = new HashMap<>();
        if(antRandom == null)
            antRandom = new Random(System.currentTimeMillis());
        xdim = xDim;
        ydim = yDim;
        if(foodPatches == null)
            foodPatches = new PointQuadTree<>(new Point(0,0), new Dimension(xdim, ydim));
        patches = new TreeSet<>();
     
        //Ordering order = new Ordering();
        for(int x = 0; x < xDim; x++)
        {
            for(int y = 0; y < yDim; y++)
            {
                final int fx = x;
                final int fy = y;
                //Patch patch = new Patch(x,y);
                ActorRef patch = AntMain.system.actorOf(new Props(new UntypedActorFactory() {
                    public UntypedActor create()
                    {
                        return new Patch(fx, fy);
                    }
                }));
                if(patch == null)
                    System.err.println("Failed to create patch.");
                patchMap.put(new Point(x,y), patch);
                patches.add(patch);
                //patchRoutes.add(patch.self().path().address().toString());
            }
        }
        
        int antX = antRandom.nextInt(xDim);
        int antY = antRandom.nextInt(yDim);
        ActorRef newAnt = AntMain.system.actorOf(new Props(new UntypedActorFactory() {
        	public UntypedActor create()
            {
        		return new Ant();
            }
        }));
        patchMap.get(new Point(antX,antY)).tell(newAnt);
        antMap.put(newAnt, new Point(antX,antY));
        newAnt.tell((Integer)antMap.size());
        newAnt.tell(getSelf());
        
        //bRouter = getContext().actorOf(new Props(Patch.class).withRouter(BroadcastRouter.apply(patches)));
        bRouter = AntMain.system.actorOf(new Props(Patch.class).withRouter(BroadcastRouter.create(patches)));
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if(o instanceof Tick)
        {
            foodPatches.clear();
        }
        if(o instanceof String){
        	if (o.equals("getDetails")){
        		getSender().tell(new GUIRequest(xdim, ydim, patches));
        		return;
        	}
        	if (o.equals("ants move")){
        		for(ActorRef ant:antMap.keySet()){
        			ant.tell(new AntMove(), getSelf());
        		}
        		return;
        	}
        }
        if(o instanceof VisionRequest){
        	VisionRequest rq = (VisionRequest)o;
        	Point center = antMap.get(getSender());
        	ArrayList<ActorRef> patches = new ArrayList<ActorRef>();
        	
        	int cx = center.x, cy = center.y;
        	patches.add(patchMap.get(new Point(cx-1, cy-1)));
        	patches.add(patchMap.get(new Point(cx, cy-1)));
        	patches.add(patchMap.get(new Point(cx+1, cy-1)));
        	patches.add(patchMap.get(new Point(cx-1, cy)));
        	patches.add(patchMap.get(new Point(cx, cy)));
        	patches.add(patchMap.get(new Point(cx+1, cy)));
        	patches.add(patchMap.get(new Point(cx-1, cy+1)));
        	patches.add(patchMap.get(new Point(cx, cy+1)));
        	patches.add(patchMap.get(new Point(cx+1, cy+1)));
        	
        	VisionRequest reply = new VisionRequest();
        	reply.center = center;
        	reply.patches = patches;
        	getSender().tell(reply);
        	return;
        	
        }
        if(o instanceof Enter){
          	System.out.println("ant " + antMap.get(getSender()).toString() + " moved to (" + ((Enter)o).endX + "," + ((Enter) o).endY + ")");
        	((Enter) o).startX = antMap.get(getSender()).x;
        	((Enter) o).startY = antMap.get(getSender()).y;
        	moveList.put(getSender(), (Enter)o);
        	Enter ent = ((Enter) o);
        	Coordinated coord = new Coordinated(ent, new Timeout(5, TimeUnit.SECONDS));
			patchMap.get(new Point(ent.endX, ent.endY)).tell(coord);
			antMap.put(getSender(), new Point(ent.endX, ent.endY));
        	
        	/*if(moveList.size() == antMap.size()){
        		for(Enter en:moveList.values()){
        			Coordinated coord = new Coordinated(en, new Timeout(5, TimeUnit.SECONDS));
        			patchMap.get(new Point(en.endX, en.endY)).tell(coord);
        			antMap.put(, value)
        		}
        		moveList.clear();
        	}*/
        	return;
        }
        if(o instanceof Eat){
        	Eat ea =new Eat();
        	ea.food=10;
        	patchMap.get(antMap.get(getSender())).tell(ea, getSender());
        	return;
        }
        if(o instanceof Scent){
        	Scent sce = (Scent)o;
        	((Scent) o).smell = 1;
        	patchMap.get(antMap.get(getSender())).tell(sce, getSender());
        	return;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
