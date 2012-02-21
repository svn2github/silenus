package com.silenistudios.silenus;

import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

import com.silenistudios.silenus.dom.BitmapInstance;
import com.silenistudios.silenus.dom.Instance;
import com.silenistudios.silenus.dom.Keyframe;
import com.silenistudios.silenus.dom.Layer;
import com.silenistudios.silenus.dom.Path;
import com.silenistudios.silenus.dom.Shape;
import com.silenistudios.silenus.dom.SymbolInstance;
import com.silenistudios.silenus.dom.Timeline;
import com.silenistudios.silenus.raw.ColorManipulation;
import com.silenistudios.silenus.raw.TransformationMatrix;

/**
 * A scene renderer will take a loaded scene from an XFLParser, and will
 * render it to an interface called through RenderInterface.
 * @author Karel
 *
 */
public class SceneRenderer {
	
	// the scene
	Timeline fScene;
	
	// the renderer
	RenderInterface fRenderer;
	
	// the highest frame found in this scene - can be seen as the animation length
	int fMaxFrameIndex;
	
	// stack of color manipulations - because they propagate through the symbol tree
	Stack<ColorManipulation> fColorManipulationStack = new Stack<ColorManipulation>();
	
	
	// constructor
	public SceneRenderer(Timeline scene, RenderInterface renderer) {
		fRenderer = renderer;
		fScene = scene;
	}
	
	// render the scene at a given frame
	public void render(int frame) {
		
		// draw the different layers in order
		Vector<Layer> layers = fScene.getLayers();
		for (Layer layer : layers) {
			drawLayer(layer, frame);
		}
	}
	
	
	// draw a layer
	private void drawLayer(Layer layer, int frame) {
		
		// walk over all keyframes
		Vector<Keyframe> frames = layer.getKeyframes();
		for (int i = 0; i < frames.size(); ++i) {
			
			// get frame
			Keyframe f1 = frames.get(i);
			
			// there's no next frame to interpolate with - don't do interpolation
			if (i+1 >= frames.size()) {
				interpolateFrames(f1, f1, frame);
				return;
			}
			
			// get the next frame and see if it's a fit
			Keyframe f2 = frames.get(i+1);
			if (f1.getIndex() <= frame && frame < f2.getIndex()) {
				
				// it's a fit, is there a tween here?
				if (f1.isTween()) {
					interpolateFrames(f1, f2, frame);
				}
				
				// no tween, just draw the frame
				else {
					interpolateFrames(f1, f1, frame);
				}
				
				// done!
				return;
			}
		}
	}
	
	
	// interpolate between two frames
	private void interpolateFrames(Keyframe f1, Keyframe f2, int frame) {
		
		// compute the distance between the two, unless it's the same frame (aka, there is no tween)
		double d = 0;
		if (f1.getIndex() != f2.getIndex()) d = (double)(frame - f1.getIndex()) / (double)(f2.getIndex() - f1.getIndex());
		
		// update d for ease
		d = f1.computeEase(d);
		
		// walk over all bitmap instances
		Collection<BitmapInstance> bitmapInstances = f1.getBitmapInstances();
		for (BitmapInstance i1 : bitmapInstances) {
			
			// get the instance in the second frame
			Instance i2 = f2.getInstance(i1.getLibraryItemName());
			
			// save transformation matrix
			fRenderer.save();
			
			// move/scale/rotate to the correct position
			transformInstance(i1, i2, d, frame);
			
			// render the image
			// if there is color manipulation, we pass it on
			
			if (fColorManipulationStack.empty()) {
				fRenderer.drawImage(i1.getBitmap());
			}
			else {
				fRenderer.drawImage(i1.getBitmap(), fColorManipulationStack.peek());
			}
			
			// done
			resetInstance(i1, i2);
			fRenderer.restore();
		}
		
		
		// walk over all symbol instances
		Collection<SymbolInstance> symbolInstances = f1.getSymbolInstances();
		for (SymbolInstance i1 : symbolInstances) {
			
			// get the instance in the second frame
			Instance i2 = f2.getInstance(i1.getLibraryItemName());
			
			// save transformation matrix
			fRenderer.save();
						
			// move/scale/rotate to the correct position
			transformInstance(i1, i2, d, frame);
			
			// render all sub-layers
			Timeline timeline = i1.getGraphic().getTimeline();
			Vector<Layer> layers = timeline.getLayers();
			for (Layer layer : layers) {
				drawLayer(layer, frame);
			}
			
			// done
			resetInstance(i1, i2);
			fRenderer.restore();
		}
		
		
		// walk over all shapes - shapes have no tweens
		// TODO is this always true?
		Collection<Shape> shapes = f1.getShapes();
		for (Shape i1 : shapes) {
			
			// save transformation matrix
			fRenderer.save();
						
			// move/scale/rotate to the correct position
			transformInstance(i1, i1, d, frame);
			
			// render the shape
			renderShape(i1);
			
			// done
			resetInstance(i1, i1);
			fRenderer.restore();
		}
	}
	
	
	// render an instance
	private void transformInstance(Instance i1, Instance i2, double d, int frame) {
		
		/**
		 * STEP 1: compute scaling and rotation interpolation
		 */
		
		/**
		 * STEP 2: interpolate the transformation point and move there for scaling and rotation
		 */
		
		
		/**
		 * STEP 1: interpolate the registration point by rotating it around the 
		 */
		// translate to the transformation point
		// NOTE: this is actually not necessary, since this is contained within the transformation matrix itself
		//fRenderer.translate(i1.getTransformationPointX(), i1.getTransformationPointY());
		
		/**
		 * STEP 2: perform transformations
		 * Note: if we are not interpolating, i1 = i2 and this still works
		 */
		
		// normal tween animation
		if (!i1.hasInBetweenMatrices()) {
			
			// interpolate scaling
			double scaleX = interpolateValues(i1.getScaleX(), i2.getScaleX(), d);
			double scaleY = interpolateValues(i1.getScaleY(), i2.getScaleY(), d);
			
			// interpolate rotation, and make sure we always rotate an angle smaller than 180� (shortest path)
			double r1 = -i1.getRotation();
			double r2 = -i2.getRotation();
			if (Math.abs(r1-r2) > Math.PI) {
				if (r1 < r2) r1 += 2 * Math.PI;
				else r2 += 2 * Math.PI;
			}
			double rotation = interpolateValues(r1, r2, d);
			
			// perform linear interpolation between the two transformation points
			// these points have already been placed, scaled and rotated in their "final" position
			double translateX = interpolateValues(i1.getTransformationPointX(), i2.getTransformationPointX(), d);
			double translateY = interpolateValues(i1.getTransformationPointY(), i2.getTransformationPointY(), d);
			
			// move to this position
			fRenderer.translate(translateX, translateY);
			
			// translate to the correct location
			//fRenderer.translate(interpolateValues(i1.getTranslateX(), i2.getTranslateX(), d), interpolateValues(i1.getTranslateY(), i2.getTranslateY(), d));
			
			// scale
			fRenderer.scale(scaleX, scaleY);
			
			// rotate
			fRenderer.rotate(rotation);
			
			// move back to registration point
			fRenderer.translate(-i1.getRelativeTransformationPointX(), -i1.getRelativeTransformationPointY());
		}
		
		/**
		 * STEP 3: perform IK transformations if available
		 */
		
		// there is an IK pose in here
		if (i1.hasInBetweenMatrices()) {
			
			// get max frame index and make sure we don't cross it
			int maxFrameIndex = i1.getMaxIKFrameIndex();
			if (frame > maxFrameIndex) frame = maxFrameIndex;
			
			// get transformation matrix
			TransformationMatrix matrix = i1.getInBetweenMatrix(frame);
			
			// perform the transformation
			// scale values are not provided by the between matrices, so we take those from our original transformation
			fRenderer.translate(matrix.getTranslateX(), matrix.getTranslateY());
			fRenderer.scale(i1.getScaleX(), i1.getScaleY());
			fRenderer.rotate(matrix.getRotation());
		}
		
		/**
		 * STEP 4: perform color transformations
		 */
		
		// there is color manipulation
		if (i1.hasColorManipulation() || i2.hasColorManipulation()) {
			
			// we interpolate between default or set color manipulation
			ColorManipulation col = ColorManipulation.interpolate(i1.getColorManipulation(), i2.getColorManipulation(), d);
			fColorManipulationStack.push(col);
		}
	}
	
	
	// restore the internal state to its original form after all subobjects are rendered
	private void resetInstance(Instance i1, Instance i2) {

		// we pop the color manipulation from the stack
		if (i1.hasColorManipulation() || i2.hasColorManipulation()) {
			fColorManipulationStack.pop();
		}
	}
	
	
	// render a shape
	private void renderShape(Shape shape) {
		
		// draw all the fill paths
		Vector<Path> fillPaths = shape.getFillPaths();
		for (Path path : fillPaths) {
			fRenderer.drawPath(path);
			fRenderer.fill(shape.getFillStyle(path.getIndex()));
		}
		
		
		// draw all the stroke paths
		Vector<Path> strokePaths = shape.getStrokePaths();
		for (Path path : strokePaths) {
			fRenderer.drawPath(path);
			fRenderer.stroke(shape.getStrokeStyle(path.getIndex()));
		}
		
	}
	
	
	// interpolate two values
	private double interpolateValues(double p1, double p2, double d) {
		return p1 + (p2 - p1) * d;
	}
}
