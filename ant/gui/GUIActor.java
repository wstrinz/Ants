package ant.gui;

import java.awt.Color;
import java.util.TreeSet;

import javax.swing.JPanel;

import scala.concurrent.stm.TMap;
import scala.concurrent.stm.Ref.View;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import ant.GetPatchInfo;
import ant.Patch;
import ant.World;

public class GUIActor extends UntypedActor {
	
	GUIBackground gui;
	TreeSet<ActorRef> patches;
	@Override
	public void onReceive(Object o) throws Exception {

		if (o instanceof ActorRef) {	
			((ActorRef) o).tell("getDetails", getSelf());
			return;
		} 
		if (o instanceof GUIRequest){
			makeGUI((GUIRequest)o);
			return;
		}
		if (o instanceof GetPatchInfo){
			if(((GetPatchInfo) o).x == -1){
				for(ActorRef p : patches){
					p.tell((GetPatchInfo) o, getSelf());
				}
			}
			else{
				int py = ((GetPatchInfo) o).y;
				int px = ((GetPatchInfo) o).x;
				Integer food = ((GetPatchInfo) o).food;
				TMap.View<Integer, ActorRef>antses = ((GetPatchInfo) o).ants;
				int i = py * gui.yD + px; 
				GUIBackground.updatePatchTT((JPanel)gui.gameBoard.getComponent(i), "(" + px + ", " + py + ") " + food);
				if (!antses.isEmpty()){
					GUIBackground.colorPatch((JPanel)gui.gameBoard.getComponent(i), Color.red);	
				}
				return;
			}
			return;
		}
		throw new UnsupportedOperationException("Not supported yet.");
	}

	
	private void makeGUI(GUIRequest gr) {
		patches = gr.patches;
		gui = new GUIBackground(gr.x, gr.y, gr.patches, getSelf());
		gui.pack();
		gui.setResizable(true);
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
	}
	
	public void getPatchInfo(ActorRef pa){
		pa.tell(new GetPatchInfo(), getSelf());
	}
}
