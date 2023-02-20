import com.neuronrobotics.bowlerkernel.Bezier3d.*;
import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import eu.mihosoft.vrl.v3d.Cylinder
import eu.mihosoft.vrl.v3d.Sphere
import eu.mihosoft.vrl.v3d.Transform
import eu.mihosoft.vrl.v3d.parametrics.LengthParameter

// code here

return new ICadGenerator(){

	@Override
	public ArrayList<CSG> generateCad(DHParameterKinematics arg0, int arg1) {
		// TODO Auto-generated method stub
		ArrayList<CSG> back =[]
		back.add(new Cube(1).toCSG())
		for(CSG c:back)
			c.setManipulator(arg0.getLinkObjectManipulator(arg1))
		return back;
	}

	@Override
	public ArrayList<CSG> generateBody(MobileBase arg0) {
		// TODO Auto-generated method stub
		ArrayList<CSG> back =[]
		back.add(new Sphere(1).toCSG())
		
		def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"
		def numBezierPieces = 20
		LengthParameter bucketDiameter = new LengthParameter("Bucket Diameter (mm)", 304.8, [0, 1000])
		LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 19, [0, 100])
		
		LengthParameter bayDepth = new LengthParameter("Bay Depth (mm)", 400, [0, 1000])
		LengthParameter bayWidth = new LengthParameter("Bay Width (mm)", 400, [0, 1000])
		LengthParameter bayHeight = new LengthParameter("Bay Height (mm)", 1000, [0, 5000])
		
		LengthParameter railElevation = new LengthParameter("Rail Elevation (mm)", 200, [0, 1000])
		LengthParameter trackDistFromWall = new LengthParameter("Track Distance from Wall (mm)", 500, [0, 1000])
		
		CSG plantShelf = new Cube(bayDepth.getMM()/2, bayWidth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayDepth.getMM()/4)
			.movey(0)
			.movez(boardThickness.getMM()/2)
			.rotz(90)
		CSG bucketGhost = new Cylinder(bucketDiameter.getMM()/2,boardThickness.getMM(), (int) 40).toCSG()
		plantShelf = plantShelf.difference(bucketGhost)
		
		def armDepth = bayDepth.getMM()/4
		def armWidth = (bayWidth.getMM()-bucketDiameter.getMM()) / 2
		BezierEditor armBez = new BezierEditor(ScriptingEngine.fileFromGit(URL, "armBez.json"),numBezierPieces)
		armBez.setStart(bucketDiameter.getMM()/2, 0, 0)
		armBez.setEnd(bayWidth.getMM()/2, armDepth, 0)
		//armBez.setCP1(bucketDiameter.getMM()/2, armDepth, 0)			Used to reset control point before manually tweaking - JMS, Feb 2023
		armBez.setCP2(bucketDiameter.getMM()/2, armDepth, 0)
		ArrayList<Transform> armTrans = armBez.transforms()
		ArrayList<CSG> armCurve = armBez.getCSG()
		CSG armRect = new Cube(armWidth, armDepth, boardThickness.getMM()).toCSG() // rectangle is temporary
			.movex(bucketDiameter.getMM()/2+armWidth/2)
			.movey(armDepth/2)
			.movez(boardThickness.getMM()/2)
		CSG armShelfPort = armRect
		CSG armShelfStarboard = armShelfPort.mirrorx()
		
		plantShelf = plantShelf.union(armShelfPort)
		plantShelf = plantShelf.union(armShelfStarboard)
		
		
		
		CSG portWall = new Cube(boardThickness.getMM(),bayDepth.getMM(),bayHeight.getMM()/2).toCSG()
			.movex(bayWidth.getMM()/2 + boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		CSG starboardWall = portWall.mirrorx()
		
		CSG backWall = new Cube(bayWidth.getMM(), boardThickness.getMM(), bayHeight.getMM()/2).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2-boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		
		//CSG railShelf = plantShelf.movez(railElevation.getMM())
		
		println("trackDistFromWall: ")
		println(trackDistFromWall.getMM())
		CSG portTrack = new Cube(trackDistFromWall.getMM(), bayDepth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()/2)
			.movey(0)
			.movez(railElevation.getMM())
		CSG starboardTrack = portTrack.mirrorx()
		CSG backTrack = new Cube(bayWidth.getMM(), trackDistFromWall.getMM(), boardThickness.getMM()).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2+trackDistFromWall.getMM()/2)
			.movez(railElevation.getMM())
		
		CSG trackShelf = portTrack.union(starboardTrack).union(backTrack)
		
		back.add(plantShelf)
		back.add(portWall)
		back.add(starboardWall)
		back.add(backWall)
		back.add(trackShelf)
		
		
		for(CSG c:back)
			c.setManipulator(arg0.getRootListener())
		for(DHParameterKinematics kin:arg0.getAllDHChains()) {
			CSG limbRoot =new Cube(1).toCSG()
			limbRoot.setManipulator(kin.getRootListener())
			back.add(limbRoot)

		}
		//back.addAll(armCurve)				Uncomment to show and edit the bezier arm curve - JMS, Feb 2023
		
		return back;
	}
	
	
}
