package us.ihmc.simulationConstructionSetTools.socketCommunication;

public enum RobotProtocolCommand
{
   HELLO, PERIOD, REQ_ALL_REGISTRIES_AND_VARIABLES, ALL_REGISTRIES_AND_VARIABLES, REGISTRY_SETTINGS, REGISTRY_SETTINGS_PROCESSED, SENT_VAR_INDICES, UPDATE_VARS,
   CHECK_CONNECTED, DISCONNECT, SET, USR_CMD, DATA, TEXT_MESSAGE;
}
