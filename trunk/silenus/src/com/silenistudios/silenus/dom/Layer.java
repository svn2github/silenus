package com.silenistudios.silenus.dom;

import com.silenistudios.silenus.ParseException;
import com.silenistudios.silenus.XFLLibrary;
import com.silenistudios.silenus.xml.XMLUtility;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.silenistudios.silenus.xml.Node;


/**
 * A layer contains a graphic (or set of graphics) and animation data
 * that is applied to these graphics.
 * @author Karel
 *
 */
public class Layer {
	
	// set of keyframes
	Vector<Keyframe> fKeyframes = new Vector<Keyframe>();
	
	// name of the layer
	String fName;
	
	// visible?
	boolean fVisible;
	
	// highest frame index
	int fMaxFrameIndex = 0;
	
	// animation type - for now only used for IK animations
	String fAnimationType;
	
	
	
	// create a layer
	public Layer(XMLUtility XMLUtility, XFLLibrary library, Node root) throws ParseException {
		
		// set type
		fAnimationType = XMLUtility.getAttribute(root, "animationType", "");
		
		// name of the layer
		fName = XMLUtility.getAttribute(root, "name");
		
		// visible or not?
		fVisible = XMLUtility.getBooleanAttribute(root,  "visible", true);
		
		// get the different keyframes
		Vector<Node> frames = XMLUtility.findNodes(root,  "DOMFrame");
		for (Node node : frames) {
			Keyframe frame = new Keyframe(XMLUtility, library, node);
			fKeyframes.add(frame);
			
			// note that we subtract 1 frame - this is because the frame itself is also counted in the duration!
			fMaxFrameIndex = frame.getIndex() + frame.getDuration() - 1;
			
			// set next key frame for the previous frame
			if (fKeyframes.size() > 1) fKeyframes.get(fKeyframes.size()-2).setNextKeyframe(frame);
		}
		
	}
	
	
	// name
	public String getName() {
		return fName;
	}
	
	
	// visible?
	public boolean isVisible() {
		return fVisible;
	}
	
	
	// get the keyframes
	public Vector<Keyframe> getKeyframes() {
		return fKeyframes;
	}
	
	
	// get all images used for animation in this timeline
	public Set<Bitmap> getUsedImages() {
		Set<Bitmap> v = new HashSet<Bitmap>();
		for (Keyframe frame : fKeyframes) v.addAll(frame.getUsedImages());
		return v;
	}
	
	
	// get animation length
	public int getMaxFrameIndex() {
		return fMaxFrameIndex;
	}
	
	
	// get type
	public String getAnimationType() {
		return fAnimationType;
	}
	
	
	// get the keyframe for a corrected frame number - this number is first pulled through SymbolInstance.getCorrectFrame
	// to account for the different loop types
	public Keyframe getKeyframe(int correctedFrame) {
		
		// look among all keyframes for a match
		for (Keyframe keyframe : fKeyframes) {
			if (keyframe.getIndex() <= correctedFrame && correctedFrame < keyframe.getIndex() + keyframe.getDuration()) return keyframe;
		}
		
		// no match found
		return null;
	}
}
