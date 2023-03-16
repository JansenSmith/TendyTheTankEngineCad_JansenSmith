import com.neuronrobotics.bowlerkernel.Bezier3d.*;
import com.neuronrobotics.bowlerstudio.BowlerStudio
import com.neuronrobotics.bowlerstudio.BowlerStudioController
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.ILinkListener
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.common.InvalidConnectionException
import com.neuronrobotics.sdk.pid.PIDLimitEvent

import eu.mihosoft.vrl.v3d.Transform

nameOfCOntroller="PathController"

class PathController{
	def numBezierPieces = 20
	boolean connected = false;
	String name

	ILinkListener ll=null;
	AbstractLink motor;
	BezierEditor editor
	BezierEditor editor2
	BezierEditor editor3
	BezierEditor editor4
	
	def unitTFs = []
	def transforms
	def tfLengths
	double total=0;
	public PathController(String n) {
		name=n
	}
	public String getName() {
		return name
	}
	public void setPath(ArrayList<TransformNR> tfs) {

		transforms=tfs

		for(int i=0;i<transforms.size()-1;i++) {
			TransformNR start = transforms.get(i)
			TransformNR end = transforms.get(i+1)
			unitTFs.add(start.inverse().times(end))
		}

		tfLengths = unitTFs.collect{
			return Math.sqrt(Math.pow(it.getX(), 2) + Math.pow(it.getY(), 2)+Math.pow(it.getZ(), 2))
		}
		println tfLengths



		for(int i=0;i<tfLengths.size();i++) {
			total+=tfLengths.get(i)
		}
		println "total length = "+total
	}
	public boolean connect(){
		connected=true;
		def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"



		editor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez.json"),numBezierPieces)
		editor2 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez2.json"),numBezierPieces)
		editor3 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez3.json"),numBezierPieces)
		editor4 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez4.json"),numBezierPieces)
		editor4.setCP2(editor4.cp2Manip.getX(),editor4.cp2Manip.getY(),50)
		editor4.setEnd(0,0,0)


		editor.addBezierToTheEnd(editor2)
		editor2.addBezierToTheEnd(editor3)
		editor3.addBezierToTheEnd(editor4)
		//editor.setStart(-100, 0, 0)

		def y=[]
		y.addAll(editor.transforms())
		y.addAll(editor2.transforms())
		y.addAll(editor3.transforms())
		y.addAll(editor4.transforms())

		setPath(y.collect{ TransformFactory.csgToNR(it)})


		MobileBase base=DeviceManager.getSpecificDevice( "TendyTheTankEngine",{
			MobileBase m = MobileBaseLoader.fromGit(
			"https://github.com/TechnocopiaPlant/TendyTheTankEngine.git",
			"TendyTheTankEngine.xml"
			)
			return m
		})

		DHParameterKinematics drive=null
		for(DHParameterKinematics k:base.getAllDHChains() ) {
			if(k.getScriptingName().contentEquals("DriveCarrage")) {
				drive=k;
			}
		}
		drive.setMaxEngineeringUnits(0, total-1)
		drive.setMinEngineeringUnits(0, 0)

		motor = drive.getAbstractLink(0)
		ll =new ILinkListener() {

			/**
			 * On link position update.
			 *
			 * @param source the source
			 * @param engineeringUnitsValue the engineering units value
			 */
			public void onLinkPositionUpdate(AbstractLink source,double engineeringUnitsValue) {
				//
				TransformNR loc = poseAtLocation( engineeringUnitsValue)
				drive.setRobotToFiducialTransform(loc)

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
		return isAvailable();
	}
	public double getTotal() {
		return total;
	}

	public Transform CADposeAtLocation(double engineeringUnitsValue) {
		return TransformFactory.nrToCSG(poseAtLocation(engineeringUnitsValue) )
	}

	public TransformNR poseAtLocation(double engineeringUnitsValue) {
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
		//println "Path.groovy update "+engineeringUnitsValue+" tf index = "+tfIndex+" unit Distance = "+unitDIstance
		TransformNR location = transforms.get(tfIndex)
		TransformNR intermediate = unitTFs.get(tfIndex).scale(unitDIstance)
		return location.times(intermediate)
	}

	/**
	 * Determines if the device is available.
	 *
	 * @return true if the device is avaiable, false if it is not
	 * @throws InvalidConnectionException the invalid connection exception
	 */
	public boolean isAvailable() throws InvalidConnectionException{
		return connected;
	}

	/**
	 * This method tells the connection object to disconnect its pipes and close out the connection. Once this is called, it is safe to remove your device.
	 */

	public void disconnect(){
		motor.removeLinkListener(ll)
		get().collect{BowlerStudioController.removeObject(it)}
		connected = false;
	}
	ArrayList<Object> get(){
		def back=[]
		back.addAll(editor.get())
		back.addAll(editor2.get())
		back.addAll(editor3.get())
		back.addAll(editor4.get())
		return back
	}
}

def pc = DeviceManager.getSpecificDevice(nameOfCOntroller, {
	def pc = new PathController(nameOfCOntroller)
	pc.connect()
	return pc
})

pc.get().collect{BowlerStudioController.addObject(it, null)}


return null
