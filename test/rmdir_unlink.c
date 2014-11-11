#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
    int size = getFreeDiskSize();
    int fp = creat("wddabc");
    assertTrue(fp>=0);
    close(fp);
    assertTrue(!mkdir("wddabcFile"));
    assertTrue(rmdir("wddabc"));
    assertTrue(unlink("wddabcFile"));
	assertTrue(!rmdir("wddabcFile"));
	assertTrue(!unlink("wddabc"));
    assertTrue(getFreeDiskSize() == size);
    done();
    return 0;
}
