import com.neuronrobotics.bowlerkernel.Bezier3d.*;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.common.DeviceManager

import eu.mihosoft.vrl.v3d.Transform

def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"


def numBezierPieces = 20
BezierEditor editor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez.json"),numBezierPieces)
BezierEditor editor2 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez2.json"),numBezierPieces)
BezierEditor editor3 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez3.json"),numBezierPieces)


editor.addBezierToTheEnd(editor2)
editor2.addBezierToTheEnd(editor3)

def transforms=[]

transforms.addAll(editor.transforms())
transforms.addAll(editor2.transforms())
transforms.addAll(editor3.transforms())

def unitTFs = [new Transform()]

for(int i=0;i<transforms.size()-1;i++) {
	Transform start = transforms.get(i)
	Transform end = transforms.get(i+1)
	unitTFs.add(start.inverse().apply(end))
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

MobileBase base=DeviceManager.getSpecificDevice( "TendyTheTankEngine",{
	MobileBase m = MobileBaseLoader.fromGit(
		"https://github.com/TechnocopiaPlant/TendyTheTankEngine.git",
		"TendyTheTankEngine.xml"
		)
	return m
})

return [
	editor.get(),
	editor2.get(),
	editor3.get()
	
]