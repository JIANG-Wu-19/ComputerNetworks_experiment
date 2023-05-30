//
//	created:2020.12.21
//	author:why

message VoicePacket{
	int source_id;
	int destination;
	
	double generation_time;	//保留时间
	double delaybound;	//到达时间
	int packet_size;	//包大小
	
	double delay;	//发包延迟
}