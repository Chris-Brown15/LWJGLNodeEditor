package cs.ui.prefabs;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.nmemFree;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

public class UINode {
	
	public static int circleSize = 8;
	
	long name;
	float value;
	int 
		ID ,
		inputCount ,
		outputCount
	;
	
	NkRect bounds = NkRect.malloc();
	NkColor color = NkColor.malloc();
	float panelX , panelY , panelW , panelH;
	
	public void shutDown() {

		if(!isFreed()) {
			
			bounds.free();
			color.free();
			nmemFree(name);
			name = NULL;
			
		}
		
	}
	
	public boolean isFreed() {
		
		return name == NULL;
		
	}
	
	//calculates the appropriate position of an output joint given an index and the set number of output joints for this node
	public NkRect jointPosition(int index , boolean output) {
		
		//joints are positioned based on their index and the total number of joints. joints are indexed such that the joint at index 0
		//is the uppermost joint, and the joint with the greatest index is the lowest joint y-wise.
		//The first joint does not start at the very top of the node. It receives some padding from the top so that it is equidistant
		//from the top corner of the node and the next joint. The same logic is applied for the last joint. It will not be placed at the
		//very bottom corner of the node, it is instead placed equidistant from the bottom of the node and the second to last joint.

		final float distanceBetweenJoints = output ? bounds.h() / (outputCount + 1) : bounds.h() / (inputCount + 1);
		
		float 
			xPos = panelX - (circleSize / 2) ,
			yPos = panelY
		;			
		
		if(output) xPos += bounds.w();
		
		yPos += distanceBetweenJoints + (distanceBetweenJoints * index);			
		yPos -= circleSize / 2;
		
		NkRect pos = NkRect.malloc(MemoryStack.stackGet()).set(xPos , yPos , circleSize , circleSize);
		
		return pos;
		
	}
	
}
