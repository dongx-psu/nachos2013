#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

char buf[100];

int main(int argc, char** argv)
{
    int size = getFreeDiskSize();
    chdir("..");
    chdir(".");
	//Assert that rmdir can't be done
    assertTrue(rmdir("nachos"));
    assertTrue(!mkdir("nachos"));
    chdir("./nachos/./../nachos/.");
    assertTrue(!rmdir("../nachos"));
    mkdir("/svn");
    mkdir("../svn/./././acm.sjtu.edu.cn");
    chdir("./.././svn/././acm.sjtu.edu.cn/./.");
    printCwd();
    chdir(".././nachos/./../tenet/../.././../");
    printCwd();
    assertTrue(rmdir("/svn"));
    chdir("./..");
    assertTrue(!rmdir("/svn/acm.sjtu.edu.cn"));
    assertTrue(!rmdir("/svn"));
    assertTrue(mkdir("jns"));
	chdir("/");
    assertTrue(getFreeDiskSize() == size);
    done();
    return 0;
}
