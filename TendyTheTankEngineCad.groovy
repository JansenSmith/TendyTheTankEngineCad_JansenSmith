import com.neuronrobotics.bowlerkernel.Bezier3d.*;
import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import eu.mihosoft.vrl.v3d.Cylinder
import eu.mihosoft.vrl.v3d.Sphere
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
		def numBezierPieces = 5
		LengthParameter bucketDiameter = new LengthParameter("Bucket Diameter (mm)", 304.8, [0, 1000])
		LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 19, [0, 100])
		LengthParameter bayDepth = new LengthParameter("Bay Depth (mm)", 400, [0, 1000])
		LengthParameter bayWidth = new LengthParameter("Bay Width (mm)", 400, [0, 1000])
		LengthParameter bayHeight = new LengthParameter("Bay Height (mm)", 1000, [0, 5000])
		
		CSG plantShelf = new Cube(bayDepth.getMM()/2, bayWidth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayDepth.getMM()/4)
			.movez(boardThickness.getMM()/2)
			.rotz(90)
		CSG bucketGhost = new Cylinder(bucketDiameter.getMM()/2,boardThickness.getMM(), (int) 40).toCSG()
		plantShelf = plantShelf.difference(bucketGhost)
		
		def armDepth = bayDepth.getMM()/4
		def armWidth = (bayWidth.getMM()-bucketDiameter.getMM()) / 2
		BezierEditor armBez = new BezierEditor(ScriptingEngine.fileFromGit(URL, "armBez.json"),numBezierPieces)
		armBez.setStart(bucketDiameter.getMM()/2, 0, 0)
		armBez.setEnd(bayWidth.getMM()/2, armDepth, 0)
		CSG armRect = new Cube(armWidth, armDepth, boardThickness.getMM()).toCSG()
			.movex(bucketDiameter.getMM()/2+armWidth/2)
			.movey(armDepth/2)
			.movez(boardThickness.getMM()/2)
		CSG armShelfPort = armRect
		CSG armShelfStarboard
		
		
		
		//CSG portWall
		//CSG starboardWall = 
		
		plantShelf = plantShelf.union(armShelfPort)
		
		back.add(plantShelf)
		
		
		for(CSG c:back)
			c.setManipulator(arg0.getRootListener())
		for(DHParameterKinematics kin:arg0.getAllDHChains()) {
			CSG limbRoot =new Cube(1).toCSG()
			limbRoot.setManipulator(kin.getRootListener())
			back.add(limbRoot)

		}
		return back;
	}
	
	
}
