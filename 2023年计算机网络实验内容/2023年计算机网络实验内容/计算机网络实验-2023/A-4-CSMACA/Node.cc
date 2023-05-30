//
//  created: 2020.12.22
//  author:why
#include "Node.h"
#define DEBUG
#ifdef DEBUG
#include <iostream>
#include <fstream>
using namespace std;

#endif

#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <stdlib.h>

vector<Node*> Node::datanodev;
double startTime;

Define_Module(Node);

Node::Node(){
    networkInitializationEvent
    = DataGenerationEvent
    = DataQueueEmptyEvent
    = DataPacketAccessEvent
    = DIFS_CheckEvent
    = CW_CheckEvent
    = DataPacketTransmissionEvent
    = TransmissionStatusOperationEvent
    = CWStatusMinusOneEvent
    = randomChooseMinislotEvent
    = NULL;
}

Node::~Node(){
    cancelAndDelete(networkInitializationEvent);
    cancelAndDelete(DataGenerationEvent);
    cancelAndDelete(DataQueueEmptyEvent);
    cancelAndDelete(DataPacketAccessEvent);
    cancelAndDelete(DIFS_CheckEvent);
    cancelAndDelete(CW_CheckEvent);
    cancelAndDelete(DataPacketTransmissionEvent);
    cancelAndDelete(TransmissionStatusOperationEvent);
    cancelAndDelete(CWStatusMinusOneEvent);
    cancelAndDelete(randomChooseMinislotEvent);

}

void Node::initialize(){
    cout<<"Initializing Start!"<<endl;
    cModule* parent = this->getParentModule();
    this->dataNodeNum = parent->par("dataNodeNum");
    this->rounds = parent->par("rounds");
    this->networkWidth = parent->par("width");
    this->networkHeight = parent->par("height");
    this->queueSize = parent->par("queueSize");
    this->DIFS = parent->par("DIFS");
    this->DIFS_checkInterval = parent->par("DIFS_checkInterval");
    this->CW_checkInterval = parent->par("CW_checkInterval");
    this->CWmin = parent->par("CWmin");
    this->datapackettransmissiontime = parent->par("data_packet_transmission_time");
    this->retryLimit = parent->par("retryLimit");
    this->backoffStageLimit = parent->par("backoffStageLimit");
    this->lamda = parent->par("lamda");
    this->datapacketSize = parent->par("packetSize");
    this->networkSpeed = parent->par("networkSpeed");

   // this->x = parent->par("node[0].x");
   // this->y = parent->par("node[0].y");
    this->setGateSize("in",this->dataNodeNum+3);
    this->setGateSize("out",this->dataNodeNum+3);
    this->getDisplayString().setTagArg("i",1,"black");

    datanodev.push_back(this);
    networkInitializationEvent = new cMessage("Network_Initialization");
    DataGenerationEvent = new cMessage("Data_Generation");
    DataQueueEmptyEvent = new cMessage("Data_Queue_Empty");
    DataPacketAccessEvent = new cMessage("Data_Packet_Access");
    DIFS_CheckEvent = new cMessage("DIFS_Check");
    CW_CheckEvent = new cMessage("CW_Check");
    DataPacketTransmissionEvent = new cMessage("Data_Packet_Transmission");
    TransmissionStatusOperationEvent = new cMessage("Transmission_Status_Operation");
    CWStatusMinusOneEvent = new cMessage("CW_Status_Minus_One");
    randomChooseMinislotEvent = new cMessage("random_Choose_Minislot");

    networkInitializationEvent->setKind(EV_NETWORKINITIALIZATION);
    this->scheduleAt(simTime()+SLOT_TIME, networkInitializationEvent);
    cout<<"Node Initialization Finished!"<<this->getId()<<endl;
    cout << "Node" << this->getId() << " x, y: " << this->x <<  " " << this->y << endl;
}

void Node::handleMessage(cMessage* msg){
    if(msg->isSelfMessage()){
        switch(msg->getKind())
           {
            case EV_NETWORKINITIALIZATION :
                networkInitialization();
                break;
            case EV_DATAGENERATION :
                dataPacketGeneration();
                break;
            case EV_DATAPACKETACCESS :
                dataPacketAccess();
                break;
            case EV_DIFSCHECK :
                DIFScheck();
                break;
            case EV_CWCHECK :
                CWcheck();
                break;
            case EV_DATAPACKETTRANSMISSION :
                DataPacketTransmission();
                break;
            case EV_DATAQUEUEEMPTY :
                IsDataQueueEmpty();
                break;
            case EV_TRANSMISSIONSTATUSOPERATION :
                TransmissionStatusZero();
                break;
            case EV_CWSTATUSMINUSONE :
                CWStatusMinusOne();
                break;
            }
        }
    else{
        delete msg;
    }
}

void Node::finish(){
    double DataThroughput = 0.0;
    double endTime = SIMTIME_DBL(simTime());

    //让其中一个节点来计算即可
    if(this->getId() == 4){
        double runningTime = (double)(endTime - startTime);
        for(int i=0;i<datanodev.size();i++){
            DataThroughput += datanodev[i]->dataSizeSum /(double)(runningTime * this->networkSpeed);
        }
        DataThroughput /= datanodev.size();
        cout<<"runningTime"<<runningTime<<endl;
        cout<<"lamda : "<<this->lamda<<endl;
        cout<<"The total number of data node is = " <<this->dataNodeNum<<endl;
        cout<<"The normalized data throughput is = "<<DataThroughput<<endl;
        cout<<"generation : "<<generation<<endl;
    }
}


void Node::networkInitialization()
{
    cout<<"Network Initialization Start"<<endl;
    this->cur_round = 0;
    this->dataqueue = 0;
    this->sentPackets = 0;
    this->lossPackets = 0;
    this->generatedPackets = 0;
    this->retryCounter = 0;
    this->CW_counter = -1;
    this->backoffStage = 0;
    this->DIFS_counter = 0;
    this->first_time_data_transmission = 0;
    this->generateddatapackets = 0;
    this->transmission_status = 0;
    this->losseddatapackets = 0;
    this->sentdatapackets = 0;
    //Busy=0;
    this->CTS_flag=0;
    this->range = 5;
    srand((unsigned)time(0));

    startTime = SIMTIME_DBL(simTime());

    dataPacketGeneration();

    //没有使用的destination，在产生数据时产生des
    this->destinationNode = getRandomDestination(this->getId());

    IsDataQueueEmpty();

    cout<<"Network Initialization Finished"<<endl;

}

void Node::dataPacketGeneration(){
    cout << "Node" << this->getId() << " start dataPacketGeneartion" << endl;
    if(this->dataQueue.empty() || (this->dataQueue.size() < this->queueSize)){
        VoicePacket* data = new VoicePacket();
        data->setSource_id(this->getId());
        data->setKind(MSG_DATA);
        data->setDestination(getRandomDestination(this->getId())->getId());
        data->setGeneration_time(simTime().dbl());
        //使数据大小满足正态分布
        int dataSize = uniform(1,5);
        data->setPacket_size(dataSize);
        this->dataQueue.push(data);
        this->dataSizeSum += dataSize;
        this->generateddatapackets++;
        generation++ ;
        cout << "DataSize " << dataSize << endl;
    }
    //使相邻两包间隔服从正态分布
    double time = exponential(1/this->lamda);
    DataGenerationEvent->setKind(EV_DATAGENERATION);
    this->scheduleAt(simTime() + time, DataGenerationEvent);
}

void Node::dataPacketAccess(){
    cout << "Node" << this->getId() << " start dataPacketAccess" << endl;
    this->cur_round++;
    //cout<<"rounds "<<cur_round<<endl;
    if(this->cur_round >= this->rounds){
        //保存结束时间
         endSimulation();
    }

    //实现RTS-CTS机制
    TransmissionFinish();
    IsDataQueueEmpty();
}


void Node::IsDataQueueEmpty(){

    if(!this->dataQueue.empty()){
        DIFS_CheckEvent->setKind(EV_DIFSCHECK);
        this->scheduleAt(simTime() + DIFS_checkInterval, DIFS_CheckEvent);
    }else{
        DataQueueEmptyEvent->setKind(EV_DATAQUEUEEMPTY);
        this->scheduleAt(simTime() + DIFS_checkInterval, DataQueueEmptyEvent);
    }
}

void Node::DIFScheck(){
    if((CTS_flag <= 0) && (DIFS_counter < 5)) {
        DIFS_counter++;
        if(DIFS_counter == 5){
            DIFS_counter = 0;
            if(first_time_data_transmission == 0){
                transmission_status = 1;
                DataPacketTransmissionEvent->setKind(EV_DATAPACKETTRANSMISSION);
                this->scheduleAt(simTime() + SLOT_TIME , DataPacketTransmissionEvent);
            }else if(first_time_data_transmission == 1){
                if(CW_counter == -1){
                    CW_counter = (int)uniform(1,getBackoffTime(backoffStage));
                }
                CW_CheckEvent->setKind(EV_CWCHECK);
                this->scheduleAt(simTime() + CW_checkInterval , CW_CheckEvent);
            }
        }else{
            DIFS_CheckEvent->setKind(EV_DIFSCHECK);
            this->scheduleAt(simTime() + DIFS_checkInterval, DIFS_CheckEvent);
        }
    }else{
        //冻结窗口时间
        DIFS_counter = 0;
        first_time_data_transmission = 1;

        DIFS_CheckEvent->setKind(EV_DIFSCHECK);
        //这里是认为数据包从一个DIFS时隙之前就开始发送了
        this->datapackettransmissiontime = this->onTrans->getPacket_size() / this->networkSpeed;
        this->scheduleAt(simTime() - DIFS_checkInterval + datapackettransmissiontime + DIFS_checkInterval , DIFS_CheckEvent);
    }
}


int Node::getBackoffTime(int backoffstage){
    int backofftime = 1;
    //遵守退避算法(i + 2)
    backofftime = (int)pow(2,(double)backoffstage + 2);
    return backofftime;
}

void Node::CWcheck(){
    if(CTS_flag <= 0 && CW_counter != 0){
        CW_counter--;
        if(CW_counter != 0){
            CW_CheckEvent->setKind(EV_CWCHECK);
            this->scheduleAt(simTime() + CW_checkInterval,CW_CheckEvent);
        }else{
            DataPacketTransmissionEvent->setKind(EV_DATAPACKETTRANSMISSION);
            this->scheduleAt(simTime() + SLOT_TIME,DataPacketTransmissionEvent);
        }
    }else if(CTS_flag > 0 && CW_counter != 0){
        DIFS_CheckEvent->setKind(EV_DIFSCHECK);
        //这里是认为数据包从一个CW时隙之前就开始发送了
        this->datapackettransmissiontime = this->onTrans->getPacket_size() / this->networkSpeed;
        this->scheduleAt(simTime() - CW_checkInterval + datapackettransmissiontime + DIFS_checkInterval, DIFS_CheckEvent);
    }
}


void Node::DataPacketTransmission(){
    cout << "Node" << this->getId() << " start DataPacketTransmission" << endl;
    this->onTrans = this->dataQueue.front();
    SendCTS(onTrans->getDestination());
    int collision_indication= 0;
    if(first_time_data_transmission == 0){
        first_time_data_transmission = 1;
        for(int i=0; i<datanodev.size(); i++){
            if(datanodev[i]->transmission_status == 1 && datanodev[i]->getId() != this->getId()){
                this->retryCounter++;
                collision_indication = 1;
                break;
            }
        }
        TransmissionStatusOperationEvent->setKind(EV_TRANSMISSIONSTATUSOPERATION);
        this->scheduleAt(simTime() + SLOT_TIME, TransmissionStatusOperationEvent);
    }else{
        for(int i=0; i<datanodev.size(); i++){
            if(datanodev[i]->CW_counter == 0 && datanodev[i]->getId() != this->getId()){
                this->retryCounter++;
                this->backoffStage++;
                collision_indication = 1;
                break;
            }
        }
        CWStatusMinusOneEvent->setKind(EV_CWSTATUSMINUSONE);
        this->scheduleAt(simTime() + SLOT_TIME, CWStatusMinusOneEvent);
    }
    if(collision_indication == 1){
        if(backoffStage > backoffStageLimit){
            backoffStage = backoffStageLimit;
        }
        if(retryCounter > retryLimit){
            this->retryCounter = 0;
            this->backoffStage = 0;
            this->dataQueue.pop();
            this->losseddatapackets++;
        }
    }else{
        this->dataQueue.pop();
        this->sentdatapackets++;
        this->backoffStage = 0;
        this->retryCounter = 0;
    }
    DataPacketAccessEvent->setKind(EV_DATAPACKETACCESS);
    this->scheduleAt(simTime() - SLOT_TIME + datapackettransmissiontime, DataPacketAccessEvent);
    TransmissionFinish();
}


Node* Node::getRandomDestination(int nodeId){
    int id;
    while(true){
        id = uniform(0,datanodev.size());
        if(datanodev[id]->getId() != nodeId){
            return datanodev[id];
        }
    }
}

void Node::TransmissionStatusZero(){
    this->transmission_status = 0;
}

void Node::CWStatusMinusOne(){
    this->CW_counter = -1;
}

/**
 * 接收数据时发送确认，将周围其他节点CTS_flag置位，发送节点帮接收节点做。
 */
void Node::SendCTS(int des){
    //让目标节点发出CTS，具体步骤就是设置他的周围的range的CTS_flag
    for(int i=0;i<datanodev.size() && i != this->getId(); i++){
        if((datanodev[des - 2]->x - datanodev[i]->x) * (datanodev[des - 2]->x - datanodev[i]->x) +
                (datanodev[des - 2]->y - datanodev[i]->y) * (datanodev[des - 2]->y - datanodev[i]->y) <= range*range){
            datanodev[i]->CTS_flag++;
         }
     }
}
/**
 *  结束传送的时候，帮接收节点把周围的CTS取消
 */
void Node::TransmissionFinish(){
    int des = onTrans->getDestination();
    for(int i=0;i<datanodev.size();i++){
        if((datanodev[des - 2]->x - datanodev[i]->x) * (datanodev[des - 2]->x - datanodev[i]->x) +
                        (datanodev[des - 2]->y - datanodev[i]->y) * (datanodev[des - 2]->y - datanodev[i]->y) <= range*range){
            datanodev[i]->CTS_flag--;
        }
    }
}


