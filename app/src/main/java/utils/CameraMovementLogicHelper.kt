package utils

class CameraMovementLogicHelper {
    val TAG = "MovementHelper"
    private var isMoving = false
    private var lastMove = -1L
    fun startMovement() {
        lastMove = System.currentTimeMillis()
    }

    fun canMoveCamera(): Boolean {
        return (System.currentTimeMillis() - lastMove) > 500
    }

}