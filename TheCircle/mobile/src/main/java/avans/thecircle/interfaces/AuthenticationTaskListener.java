package avans.thecircle.interfaces;

import avans.thecircle.utilities.ReponseState;

public interface AuthenticationTaskListener {
   void onAuthResponse(ReponseState state, String token, String userId);
}
