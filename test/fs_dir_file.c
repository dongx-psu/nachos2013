#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#include "filesysgrader.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int src, dst, amount;

//  printf("making dir\n");
  assertTrueWMsg(mkdir("dir") == 0,"mkdir error :dir");
//  printf("creat file\n");
  assertTrueWMsg((src=creat("file")) != -1,"creat error :/file");
//  printf("open dir\n");
  assertTrueWMsg((dst=open("dir")) == -1,"open error :/dir/");
//  printf("creat dir\n");
  assertTrueWMsg((dst=creat("dir")) == -1,"creat error :/dir");
//  printf("remove dir\n");
  assertTrueWMsg(rmdir("dir") == 0,"rmdir error :dir");
//  printf("creat dir\n");
  assertTrueWMsg((dst=creat("dir")) != -1,"creat error :/dir");
//  printf("chdir dir\n");
  assertTrueWMsg((chdir("dir")) == -1,"chdir error :/dir");
  done();
  return 0;
}
