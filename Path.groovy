// code here
def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"

def numBezierPieces = 15
BezierEditor editor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez.json"),10)
BezierEditor editor2 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez2.json"),numBezierPieces)
BezierEditor editor3 = new BezierEditor(ScriptingEngine.fileFromGit(URL, "bez3.json"),numBezierPieces)


editor.addBezierToTheEnd(editor2)
editor2.addBezierToTheEnd(editor3)
