package cs.ui.prefabs;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;

import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_LEFT;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_TITLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_SCALABLE;
import static org.lwjgl.nuklear.Nuklear.NK_STATIC;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_MOVABLE;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_CENTERED;
import static org.lwjgl.nuklear.Nuklear.nk_begin_titled;
import static org.lwjgl.nuklear.Nuklear.nk_contextual_begin;
import static org.lwjgl.nuklear.Nuklear.nk_contextual_end;
import static org.lwjgl.nuklear.Nuklear.nk_contextual_item_label;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_fill_circle;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_input_has_mouse_click_down_in_rect;
import static org.lwjgl.nuklear.Nuklear.nk_input_is_mouse_hovering_rect;
import static org.lwjgl.nuklear.Nuklear.nk_input_is_mouse_released;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_begin;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_bounds;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_push;
import static org.lwjgl.nuklear.Nuklear.nk_stroke_curve;
import static org.lwjgl.nuklear.Nuklear.nk_stroke_line;
import static org.lwjgl.nuklear.Nuklear.nk_window_get_bounds;
import static org.lwjgl.nuklear.Nuklear.nk_window_get_canvas;
import static org.lwjgl.nuklear.Nuklear.nk_window_get_content_region;
import static org.lwjgl.nuklear.Nuklear.nk_window_get_panel;
import static org.lwjgl.nuklear.Nuklear.nk_window_is_closed;
import static org.lwjgl.nuklear.Nuklear.nnk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_button_label;
import static org.lwjgl.nuklear.Nuklear.nk_propertyi;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import java.util.List;

import org.joml.Vector4i;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkInput;
import org.lwjgl.nuklear.NkPanel;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

/**
 * 
 * Implementation of a Node Editor in LWJGL Java using the Nuklear UI library. 
 * After initializing the backend (initializing Nuklear, OpenGL, and GLFW), create an instance of this class and call 
 * {@linkplain NodeEditor#layoutNodeEditor(long, NkContext) layoutNodeEditor()} when appropriate.
 * <br><br>
 * Make sure to call {@linkplain NodeEditor#shutDown() shutDown()} when finished with a node editor, and call 
 * {@linkplain NodeEditor#finalShutDown() finalShutDown()} when more node editors are wanted.
 * <br><br>
 * TODO: Make nodes customizable, i.e., give them the ability to have their own code. <br>
 * TODO: Fix a few simple bugs like when a joint is removed who had a link connected to it.
 *   
 * <br><br>
 * Based on: https://github.com/vurtun/nuklear/blob/master/demo/node_editor.c
 *
 */
public class NodeEditor {
	
	private static final NkColor UIElementColors = NkColor.malloc().set((byte) 100 , (byte) 100 , (byte) 100 , (byte) -1); 
	
	public static void finalShutDown() {
		
		UIElementColors.free();
		
	}
	
	private boolean 
		showGrid = true ,
		isFreed = false;
	;

	private float
		prevCursorX ,
		prevCursorY ,
		currCursorX ,
		currCursorY
	;
	
	public int options = NK_WINDOW_TITLE|NK_WINDOW_NO_SCROLLBAR|NK_WINDOW_SCALABLE;
	
	private final List<UINode> nodes;
	private final List<UINodeLink> links;
	
	private final NodeLinking linking = new NodeLinking();
	
	private UINode selected;
	private NkRect bounds = NkRect.malloc();
	private NkVec2 scroll = NkVec2.calloc();

	public NodeEditor(List<UINode> nodes , List<UINodeLink> links) {

		this.nodes = nodes;
		this.links = links;
		
		add("Node 1" , new Vector4i(50 , 50 , 170 , 170) , new Vector4i(-1 , -1 , -1 , -1) , 3 , 3);
		add("Node 2" , new Vector4i(250 , 50 , 170 , 170) , new Vector4i(-1 , -1 , -1 , -1) , 3 , 3);
			
	}
	
	private UINode find(int ID) {
		
		for(int i = 0 ; i < nodes.size() ; i ++) if(nodes.get(i).ID == ID) return nodes.get(i);			
		return null;
		
	}
	
	private void add(String name , Vector4i bounds , Vector4i color , int inCount , int outCount) {
		
		UINode node = new UINode();
		node.ID = nodes.size();
		node.value = 0;
		node.color.set((byte)color.x , (byte) color.y , (byte) color.z , (byte) color.w);
		node.inputCount = inCount;
		node.outputCount = outCount;
		node.bounds.set(bounds.x , bounds.y , bounds.z , bounds.w);
		node.name = memAddress(memUTF8(name));

		nodes.add(node);
		
	}
	
	private void link(int inID , int inSlot , int outID , int outSlot) {
		
		UINodeLink link = new UINodeLink(inID , inSlot , outID , outSlot);
		links.add(link);
		
	}

	private boolean canLink(NkInput input , NkRect circle , UINode iter) {

		return nk_input_is_mouse_released(input , NK_BUTTON_LEFT) &&
			   nk_input_is_mouse_hovering_rect(input , circle) && 
			   linking.active && linking.node != iter;
	
	}
	
	private boolean canBeginLink(NkInput input , UINode iter , NkRect circle) {
		
		return (selected == null || selected == iter) && 
			   nk_input_has_mouse_click_down_in_rect(input , NK_BUTTON_LEFT , circle , true);
	
	}
	
	private boolean canMoveGroup(NkInput input , long window , UINode iter) {

		return (selected == null || selected == iter) &&
			   glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS &&
			   nk_input_is_mouse_hovering_rect(input , iter.bounds);
	
	}
	
	public boolean layoutNodeEditor(final long window , final NkContext context) {
		
		try(MemoryStack stack = MemoryStack.stackPush()) {

			NkRect totalSpace = NkRect.malloc(stack);
			NkInput input = context.input();
			NkCommandBuffer canvas;
			
			if(nk_begin_titled(context , "NODE_EDIT" , "Node Edit" , NkRect.malloc(stack).set(0 , 0 , 800 , 600) , options)) {
				
				canvas = nk_window_get_canvas(context);
				totalSpace = nk_window_get_content_region(context , totalSpace);
				nk_layout_space_begin(context , NK_STATIC , totalSpace.h() , nodes.size());
				
				NkRect size = NkRect.malloc(stack);
				nk_layout_space_bounds(context , size);
				
				if(showGrid) {
					
					float 
						x ,
						y ,
						gridSize = 32.0f
					;
					
					//gridlines
					NkColor gridColor = NkColor.malloc(stack).set((byte) 50, (byte) 50, (byte) 50 , (byte) -1);
					for(x = size.x() - scroll.x() % gridSize ; x < size.w() ; x += gridSize) { 
					
						nk_stroke_line(canvas , x + size.x() , size.y() , x + size.x() , size.y() + size.h() , 1.0f , gridColor);
						
					}
					
					for(y = size.y() - scroll.y() % gridSize ; y < size.h() ; y += gridSize) {
						
						nk_stroke_line(canvas , size.x() , y + size.y() , size.x() + size.w() , y + size.y() , 1.0f , gridColor);
						
					}
					
				}
				
				nodes.forEach(iter -> {
					
					NkPanel node = null;
					
					nk_layout_space_push(context , iter.bounds);
					
					//node windows
					if(nnk_group_begin(context.address() , iter.name , NK_WINDOW_MOVABLE|NK_WINDOW_BORDER|NK_WINDOW_TITLE)) {
						
						node = nk_window_get_panel(context);
						
						//Nodes laid out here node contents
						
						nk_layout_row_dynamic(context , 25 , 1);						
						if(nk_button_label(context , "Button")) System.out.println("Hello Node " + iter.ID);
						
						nk_layout_row_dynamic(context , 20 , 1);
						iter.inputCount = nk_propertyi(context , "Input Slots" , 0 , iter.inputCount , 999 , 1 , 0.3f);
						
						nk_layout_row_dynamic(context , 20 , 1);
						iter.outputCount = nk_propertyi(context , "Output Slots" , 0 , iter.outputCount , 999 , 1 , 0.3f);
						
						nk_group_end(context);
						
					}
							
					//movable aspect to nodes
					
					if(canMoveGroup(input, window, iter)) {
						
						float 
							deltaX = (currCursorX - prevCursorX) ,
							deltaY = (currCursorY - prevCursorY)
						;
						
						int
							xResult = (int) (iter.bounds.x() + deltaX) ,
							yResult = (int) (iter.bounds.y() + deltaY)
						;
						
						if(xResult < 0) xResult = (int) iter.bounds.x();
						if(yResult < 0) yResult = (int) iter.bounds.y();
						
						iter.bounds.set(xResult , yResult , iter.bounds.w() , iter.bounds.h());
						
						selected = iter;
										
					} else if (!(glfwGetMouseButton(window , GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS)) selected = null;
					
					//output joints
					
					for(int i = 0 ; i < iter.outputCount ; ++i) {

						NkRect circle = iter.jointPosition(i , true);
						
						nk_fill_circle(canvas , circle , UIElementColors);
						
						//start linking
						if(canBeginLink(input, iter, circle)) {
							
							linking.active = true;
							linking.node = iter;
							linking.inputID = iter.ID;
							linking.inputSlot = i;
							selected = iter;
							
						}
						
						//draw curve from linked node
						if(linking.active && linking.node == iter && linking.inputSlot == i) {
							
							NkVec2 mousePos = input.mouse().pos();
							final float circleRadius = UINode.circleSize >> 1;
							
							/*
							 * Draws a curved line.                                                                                      
							 * The first parameter is the drawing command data structure Nuklear uses.
							 * The first two position parameters are an endpoint of the line.                                            
							 * The next four parameters represent the curvyness of the line. The first two are curvyness around the
							 * first endpoint already given, and the second two are the curvyness around the other endpoint.             
							 * the next two parameters are the position of the second endpoint of the line                               
							 * the next parameter is the line thickness, and the last parameter is the color of the line.                
							 *
							 */							
							nk_stroke_curve(
								canvas , 
								circle.x() + circleRadius , 
								circle.y() + circleRadius , 
								circle.x() + 50.0f , 
								circle.y() , 
								mousePos.x() - 50.0f ,
								mousePos.y() , 
								mousePos.x() + circleRadius , 
								mousePos.y() + circleRadius , 
								1.0f , 
								UIElementColors
							);
							
						}
						
					}

					//input circles
					for(int i = 0 ; i < iter.inputCount ; ++i) {
						
						NkRect circle = iter.jointPosition(i, false);
						
						nk_fill_circle(canvas , circle , UIElementColors);
						
						if(canLink(input, circle, iter)) {
							
							linking.active = false;
							link(linking.inputID , linking.inputSlot , iter.ID , i);
							
						}
						
					}

					iter.panelX = node.bounds().x();
					iter.panelY = node.bounds().y();
					iter.panelW = node.bounds().w();
					iter.panelH = node.bounds().h();
					
				});
					
				
				//reset linking connection
				if(linking.active && nk_input_is_mouse_released(input , NK_BUTTON_LEFT)) {
					
					linking.active = false;
					linking.node = null;
					System.err.println("Linking Failed");
					
				}
				
				links.forEach(link -> {
					
					UINode
						sender = find(link.inputID()) ,
						receiver = find(link.outputID())
					;
					
					NkRect 
						senderCircle = sender.jointPosition(link.inputSlot(), true) ,
						receiverCircle = receiver.jointPosition(link.outputSlot() , false)
					;

					final float circleRadius = UINode.circleSize >> 1;
				
					nk_stroke_curve(
						canvas , 
						senderCircle.x() + circleRadius , 
						senderCircle.y() + circleRadius , 
						senderCircle.x() + 50 , 
						senderCircle.y() ,
						receiverCircle.x() - 50 , 
						receiverCircle.y() , 
						receiverCircle.x() + circleRadius , 
						receiverCircle.y() + circleRadius , 
						1.0f , 
						UIElementColors
					);
										
				});
				
				if(nk_contextual_begin(
					context , 
					0 , 
					NkVec2.malloc(stack).set(100 , 220) , 
					nk_window_get_bounds(context , NkRect.malloc(stack))
				)) {
				
					final String[] GRID_OPTIONS = {"Show Grid" , "Hide Grid"};
					nk_layout_row_dynamic(context , 25 , 1);
					if(nk_contextual_item_label(context , "New" , NK_TEXT_ALIGN_CENTERED)) { 
					
						//the - 30 is a magic number needed because otherwise the new one spawns a bit below the cursor's y coord
						add(
							"New" , 							
							new Vector4i((int) currCursorX , (int) currCursorY - 30 , 180 , 220) , 
							new Vector4i((byte)-1 , (byte)-1 , (byte)-1 , (byte)-1) , 1 , 
							2
						);
					
					}
					
					if(nk_contextual_item_label(context , GRID_OPTIONS[showGrid ? 1 : 0] , NK_TEXT_ALIGN_CENTERED)) showGrid = !showGrid;
					
					nk_contextual_end(context);
					
				}
				
			}
			
			nk_layout_space_end(context);

			//scrolls all nodes of the graph together and the grid lines
			
			if(
				nk_input_is_mouse_hovering_rect(input , nk_window_get_bounds(context , NkRect.malloc(stack))) && 
				glfwGetMouseButton(window , GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS
			) {

				float 
					deltaX = (currCursorX - prevCursorX) ,
					deltaY = (currCursorY - prevCursorY) ,
					finalDeltaX = deltaX != Float.NEGATIVE_INFINITY ? deltaX : 0 , 
					finalDeltaY = deltaY != Float.NEGATIVE_INFINITY ? deltaY : 0
					
				;
				
				scroll.x(scroll.x() + deltaX);
				scroll.y(scroll.y() + deltaY);
				
				nodes.forEach(iter -> {
					
					iter.bounds.x(iter.bounds.x() + finalDeltaX);
					iter.bounds.y(iter.bounds.y() + finalDeltaY);
					
				});
		
			}

			prevCursorX = currCursorX;
			prevCursorY = currCursorY;
			NkVec2 cursorPos = context.input().mouse().pos();
			currCursorX = cursorPos.x();
			currCursorY = cursorPos.y();
			
		}
		
		nk_end(context);
		return !nk_window_is_closed(context , "NODE_EDIT");
		
	}
	
	public void shutDown() {

		if(!isFreed()) {
			
			for(UINode x : nodes) if(x != null) x.shutDown();
			bounds.free();
			scroll.free();
			isFreed = true;
			
		}
		
	}

	public boolean isFreed() {
		
		return isFreed;
		
	}
		
	public class NodeLinking { 
		
		boolean active;
		
		int
			inputID ,
			inputSlot
		;
		
		UINode node;
		
	}

}
