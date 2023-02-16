import com.neuronrobotics.bowlerkernel.Bezier3d.*;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.ILinkListener
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.pid.PIDLimitEvent

import eu.mihosoft.vrl.v3d.Transform

def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"


def numBezierPieces = 5
BezierEditor editor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez.json"),numBezierPieces)
BezierEditor editor2 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez2.json"),numBezierPieces)
BezierEditor editor3 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez3.json"),numBezierPieces)


editor.addBezierToTheEnd(editor2)
editor2.addBezierToTheEnd(editor3)

def y=[]

y.addAll(editor.transforms())
y.addAll(editor2.transforms())
y.addAll(editor3.transforms())

def transforms= y.collect{ TransformFactory.csgToNR(it)} 

def unitTFs = []

for(int i=0;i<transforms.size()-1;i++) {
	TransformNR start = transforms.get(i)
	TransformNR end = transforms.get(i+1)
	unitTFs.add(start.inverse().times(end))
}

def tfLengths = unitTFs.collect{
	return Math.sqrt(Math.pow(it.getX(), 2) + Math.pow(it.getY(), 2)+Math.pow(it.getZ(), 2))
}
println tfLengths

double total=0;

for(int i=0;i<tfLengths.size();i++) {
	total+=tfLengths.get(i)
}
println "total length = "+total

MobileBase base

if(args!=null)
	base=args[0]
else {
	base=DeviceManager.getSpecificDevice( "TendyTheTankEngine")
	if(base!=null)
		base.disconnect()
	base=DeviceManager.getSpecificDevice( "TendyTheTankEngine",{
		MobileBase m = MobileBaseLoader.fromGit(
			"https://github.com/TechnocopiaPlant/TendyTheTankEngine.git",
			"TendyTheTankEngine.xml"
			)
		return m
	})
}

DHParameterKinematics drive=null
for(DHParameterKinematics k:base.getAllDHChains() ) {
	if(k.getScriptingName().contentEquals("DriveCarrage")) {
		drive=k;
	}
}
drive.setMaxEngineeringUnits(0, total-1)
drive.setMinEngineeringUnits(0, 0)

AbstractLink motor = drive.getAbstractLink(0)
ILinkListener ll =new ILinkListener() {
	
	/**
	 * On link position update.
	 *
	 * @param source the source
	 * @param engineeringUnitsValue the engineering units value
	 */
	public void onLinkPositionUpdate(AbstractLink source,double engineeringUnitsValue) {
		//
		
		double distance=0;
		int tfIndex=-1;
		double unitDIstance=0;
		for(int i=0;i<tfLengths.size();i++) {
			double distStart = distance
			distance+=tfLengths.get(i)
			if(distance>engineeringUnitsValue && tfIndex<0) {
				tfIndex=i;
				unitDIstance = 1-(distance-engineeringUnitsValue)/tfLengths.get(i)
				break;
			}
		}
		println "Path.groovy update "+engineeringUnitsValue+" tf index = "+tfIndex+" unit Distance = "+unitDIstance
		TransformNR location = transforms.get(tfIndex)
		TransformNR intermediate = unitTFs.get(tfIndex).scale(unitDIstance)
		drive.setRobotToFiducialTransform(location.times(intermediate))	
	}
	
	/**
	 * On the event of a limit, this is called.
	 *
	 * @param source the source
	 * @param event the event
	 */
	public void onLinkLimit(AbstractLink source,PIDLimitEvent event) {
		
	}
}
motor.addLinkListener(ll)
motor.fireLinkListener(motor.getCurrentEngineeringUnits())

if(drive==null)
	throw new RuntimeException("Dive secion is missing, can not contine!");
	
//drive.

return [
	editor.get(),
	editor2.get(),
	editor3.get()
	
]