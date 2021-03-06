==================================
* TransparentGL Design Breakdown *
==================================

1. Design Pattern
-----------------

	It's hard to design a game blending the works of rendering and calculating together. For instance, it would be a chaos when you try to put the work of drawing a game character (view), calculating the coordinate, health, blablabla, of the character (model), and processing the control signal sent by the user.

	The work would be easier when you try to separate the drawing of the graphics and the information, the state of the graphics, and the interactive input. And we create MVC model, where model stores the state, view draw the graphics, and controller notifies model when accepting user inputs. MVC is popular in GUI design, so is our library.

	Taking thread save into consideration (thread save mechanism is provided by LWJGL, which is the depending level of our library.), we ought to separate the rendering work from the controlling work. Or the graphic drawing and sound playing work, from controlling the position of the graphics, the volume of the sound, etc.

	Actually, we are forced to do that, all rendering works of our library need OpenGL context, which is provided by the graphic environment, and could not be shared through out threads. OpenAL, OpenCL contexts are analogous. We tends to put the work of rendering inside context, which is called "Online", and the work of calculating and modifying outside context, which is called "Offline".

	(We would just take graphics in the library as the target, audio will be analogous.)

	We start from online and offline concept to design our library. Methods related to graphic drawing will be called under OpenGL context, and methods related to graphic state control will be called outside the context. (Though it could be called under context.)

2. Online VS. Offline
---------------------

	The work of a graphic object varies, some might draw a box, some might shade the scene, however there's a common abstraction for every graphic objects.

	A graphic object might needs to initialize itself when it is added into a graphic context, so it's common for a graphic to own a initialize function, or Drawable.onInit in our library. It might need to repaint itself when screen updates, so a repaint function will be need, or Drawable.onUpdate in our library. And it might need to destroy the resources when it leaves its context, so a de-constructor will be needed, or Drawable.onDestroy in our library.

		+------------------------------------+                 +------------------------+
		|          <<interface>>             |                 |     <<interface>>      |
		|             Drawable               |   <<call>>      |        Context         |
		+====================================+ <---------------+========================+
		|+ onInit(parent: Container): void   |                 |+ initContext(): void   |
		|+ onUpdate(parent: Container): void |                 |+ update(): void        |
		|+ onDestroy(parent: Container): void|                 |+ releaseContext(): void|
		+------------------------------------+                 +------------------------+
	(The context interface does not actually exists in our library, but is provided by its depending libraries, and is also transparent to us.)
	
	Now we have 3 online functions for all graphic objects, the onInit which is called when the graphic object is added into the scene, the onDraw which is called when repaint is needed, and the onDestroy when it leave its context.

	However, It's still not abundant for our design. It is inconvenience and abstract without managing the graphic objects into some structure. So in our library, we adopt a structure of tree, or a hierarchical structure.

3. Hierarchical Structure
-------------------------

	When you draw a graphic, besides drawing points, lines, polygons, or even models, we are likely to arrange glyphs into groups, so that they could be easily transformed. For example, we would like to render a human character in the game, and we wish we could move the human as a group when we want to change its coordinate, and we wish we could rotate its arms, fingers when we want the character to have specific posture.

	Thus we arrange the graphics into hierarchical structure. The context just need to paint the top level container and regard it as a entity, the container will pose rendering callback to its containing elements. It would be another container, or just an end graphic object. Visualizing the structure, we could see the top level container (provided by the rendering context) as the root, the end graphic elements as leaves, and containers as branches.

	                                          <<instance_of>> +-------------------------+
	+----------+       +-------------+           +----------|>|        Drawable         |
	| Context  |<|-----|   context   |           |            +=========================+
	+----------+       +------+------+           |                         ^
	                          |<<call>>          |                         +  <<extend>>
	                          v                  |        +-------------------------------------+
	               +---------------------+ <<generalize>> |           <<interface>>             |
	               | top level container | ------+-----|> |             Container               |
	               +---------------------+       |        +=====================================+
	            <<call>>|    ...    |            |        |+ register(drawable: Drawable):void  |
	                    v           v            |        |+ unregister(drawable: Drawable):void|
	              +---------+    /---------/     |        +-------------------------------------+
	              |container|   / graphic / -----+
	              +---------+  /---------/       |
	                   |<<call>>                 |
	                   v                         |
	              /-------/                      |
	             /graphic/  .... ----------------+
	            /-------/
	(The actual method of registering and unregistering drawables will vary from different container implementation, and different container may provide more functions.)
 
	When a drawable element is added to the container, it will be initialized once being attached to the context and being controlled by its container element or context. And its de-constructor method will be called once being detached from its parent.
 
	A drawable could be attached again once removed, which means controlling graphics as modules will be possible. For example, a game may have different scenes, inside each scene the things to draw will be the same, so you could put a scene into a container, and switch between them through attach-detach methods.

4. Resource Management
----------------------

	Though Java provides garbage collector which simplifies the design of object-oriented program, resources still need to be managed. The greatest problem for resource management comes from the thread save usage of contexts. Many resources could only be created and destroyed under contexts, and garbage collector could not recycle these resources, and may cause resource leak.

	Management of resources will be hard as the scope of a resource and its owner object may be different. For example, you might use the same character model in the game to draw many NPCs of the same kind. The model may have its own vertices, normals, texture coordinates, textures, etc. These will always be stored in the VRAM and it would not be recycled until program request the recycle of resource, or the context is destroyed. Though not recycling resources will not do harm to your computer (or your users), it might burn out of precious VRAM spaces and cause
misbehaviors of your program.

	There's another simplest model for resource management, which is called "resource pack". A resource pack will be a container as we introduced previously, so it could manage the birth and death of the resources. We call it a "pack" since we would like to create and remove a set of resources in a time. These resources should have some linkages, like they are all data of a scene, so they could be removed together with their scene.

	Users of our library could manage the resource by themselves, since we can't figure out the best way of controlling resources.
