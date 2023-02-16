# LWJGLNodeEditor
 Implementation of a node editor using the Nuklear UI library in LWJGL Java. Given an already-initialized backend, this creates a node editor 
 based on https://github.com/vurtun/nuklear/blob/master/demo/node_editor.c . 
# Usage
 Initialize GLFW, OpenGL, and Nuklear, then create an instance of NodeEditor. Free its associated memory with shutDown(), and free its static memory with finalShutDown(). This is intended as a simple implementation based on the linked example. This is written in the same style as the original node editor, with changes and modifications to better suit Java, but is ultimately attempting to achieve the same thing.

