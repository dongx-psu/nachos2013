#include "filesysgrader.h"
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024
#define BUFSIZE_s 50
char loc[BUFSIZE+1];
char loc2[BUFSIZE+1];
char buf[BUFSIZE+1];
char buf2[BUFSIZE_s];
int main(int argc, char** argv)
{
// if your file system doesn't support more than count files per dictory
// you must be failed
  printf("tester is surly slow, be patient.\n");
  int i=0;
  int count = readParameter(0);
// phase 1 directory create
  for(i=0;i<count;i++)
  {
    sprintf(loc2,"%d\0",i);
    printf("Creating %s\n",loc2);
    assertTrueWMsg(mkdir(loc2)==0,"Create failed");		
  }
  assertTrueWMsg(chdir(loc2)==0,"Create error");
  assertTrueWMsg(chdir("/1")==0,"Create error 2");
  done();
  return 0;
}
