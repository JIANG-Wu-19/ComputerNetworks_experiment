//
// created:2020.12.21
// author:why

#ifndef NODE_H_
#define NODE_H_
#include <omnetpp.h>
#include <vector>
#include <string>
#include <queue>
#include "VoicePacket_m.h"

using namespace std;
using namespace omnetpp;

#define EV_NETWORKINITIALIZATION 1
#define EV_DATAGENERATION 9
#define EV_DATAPACKETACCESS 10
#define EV_DIFSCHECK 11
#define EV_CWCHECK 12
#define EV_DATAPACKETTRANSMISSION 13
#define MSG_DATA 14
#define EV_DATAQUEUEEMPTY 15
#define EV_TRANSMISSIONSTATUSOPERATION 16
#define EV_CWSTATUSMINUSONE 17
#define EV_RANDOMCHOOSEMINISLOT 19

#define SLOT_TIME 0.00001

class Node:public cSimpleModule

{
private:
    double x = (int)uniform(0,10) ;   // the abscissa of this node
    double y = (int)uniform(0,10);   // the ordinate of this node
    double networkWidth;
    double networkHeight;
    double rounds;
    int dataNodeNum;
    int cur_round;
    int queueSize;
    int retryLimit;
    int retryCounter;
    int backoffStageLimit;
    int backoffStage;
    int CTS_flag ;
    int range ;

    double DIFS;
    double DIFS_checkInterval;
    int DIFS_counter;
    int CWmin;
    int CW_counter;
    double CW_checkInterval;
    double datapackettransmissiontime;
    int first_time_data_transmission;
    double lamda;
    double datapacketSize;
    double networkSpeed;

    queue<VoicePacket*> dataQueue;
    Node* destinationNode;

    cMessage* networkInitializationEvent;
    cMessage* DataGenerationEvent;
    cMessage* DataQueueEmptyEvent;
    cMessage* DataPacketAccessEvent;
    cMessage* DIFS_CheckEvent;
    cMessage* CW_CheckEvent;
    cMessage* DataPacketTransmissionEvent;
    cMessage* TransmissionStatusOperationEvent;
    cMessage* CWStatusMinusOneEvent;
    cMessage* randomChooseMinislotEvent;

    void networkInitialization();
    int getBackoffTime(int);

    void dataPacketGeneration();
    void DIFScheck();
    void CWcheck();
    void IsDataQueueEmpty();
    void dataPacketAccess();
    void DataPacketTransmission();
    void TransmissionStatusZero();
    void CWStatusMinusOne();
    void randomChooseMinislot();
    void create_link(Node* outNode, Node* inNode);
    void disconnectLinks();
    void SendCTS(int des);
    void TransmissionFinish();
    Node* getRandomDestination(int nodeId);

protected:
    virtual void initialize();
    virtual void handleMessage(cMessage* msg);
    virtual void finish();

public:
    static vector<Node*> datanodev; //the global vector filled with all data node instances

    long generatedPackets;
    long sentPackets;
    long lossPackets;   //丢失的所有包的记录，呃，在我的实现实现机制里面，CTS不会失败，不过也没关系，这个包的量很小，不会产生实质性影响
    long lastTimeslot;
    long dataqueue;
    int minislot_sequence_id;
    int losseddatapackets;  //丢失的数据包的记录
    double sentdatapackets;
    long generateddatapackets;
    int transmission_status;
    //包的总大小
    double dataSizeSum;
    //保存当前正在发送的包
    VoicePacket* onTrans;

    Node();
    ~Node();
};
long generation;
double dataAverageSize;
#endif

