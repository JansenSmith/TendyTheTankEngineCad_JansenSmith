import com.neuronrobotics.sdk.addons.kinematics.DHChain
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR

return new DhInverseSolver() {

	@Override
	public double[] inverseKinematics(TransformNR arg0, double[] arg1, DHChain arg2) {
		return arg1;
	}
	
}