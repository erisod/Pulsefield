#include "oschandler.h"
#include "calibration.h"
#include "dbg.h"

void usage(int argc,char *argv[]) {
    fprintf(stderr, "Usage: %s [-P port] [-n nlaser] [[-D filename] -d debug]\n",argv[0]);
    fprintf(stderr,"\t-P port\t\t\tset port to listen on (default: 7780)\n");
    fprintf(stderr,"\t-s\t\tsimulate lasers\n");
    fprintf(stderr,"\t-n nlaser\t\tnumber of laser devices\n");
    fprintf(stderr,"\t-d debug\t\tset debug option (e.g -d4, -dLaser:4)\n");
    fprintf(stderr,"\t-D file\t\tset debug filename (default Debug.out)\n");
    exit(1);
}

int main(int argc, char *argv[]) {
    int ch;
    int nproj=2;
    int port=7780;
    bool simulate=false;
    SetDebug("THREAD:1");   // Print thread names in debug messages, if any

    while ((ch=getopt(argc,argv,"d:n:P:D:s"))!=-1) {
	switch (ch) {
	case 'p':
	    port=atoi(optarg);
	    break;
	case 'n':
	    nproj=atoi(optarg);
	    break;
	case 'd':
	    SetDebug(optarg);
	    break;
	case 'D':
	    SetDebug("xxx:1",optarg);
	    break;
	case 's':
	    simulate=true;
	    break;
	default:
	    usage(argc,argv);
	}
    }
    argc-=optind;
    argv+=optind;
     
    if (argc>0)
	usage(argc,argv);

    
    Calibration::initialize(nproj);
    
    dbg("main",1) << "Creating OSCHandler on port " << port << std::endl;
    OSCHandler osc(port);

    dbg("main",1) << "Wait forever..." << std::endl;
    osc.wait();
    dbg("main",1) << "Returned from wait forever." << std::endl;
}
