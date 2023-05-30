//
//	created:2020.12.21
//	author:why

network HybridMAC{
	parameters:
	    double width;
	    double height;	//网络
	    int dataNodeNum;	//节点数目
	    int queueSize;
	    double rounds;
	    
	    double CW_checkInterval;	//碰撞窗口分时隙长
	    int CWmin;					//碰撞窗口总时长
	    double DIFS_checkInterval;	//DIFS分时隙长
	    double DIFS;				//DIFS总长
	    double data_packet_transmission_time;	//数据包传输时间
	    double conflict_time;
	    int retryLimit;				//数据包冲突后，(重传)次数限制
	    int backoffStageLimit;		//二进制退避指数限制
	    double lamda;				//数据包产生，柏松分布
	    double packetSize;			//数据包大小
	    double networkSpeed;		//网络传播速度
	    @display("bgi=background/streetmap,s;bgb=10,10");
	submodules:
	    node[dataNodeNum]:Node{
	    }
	    
	connections allowunconnected:
}