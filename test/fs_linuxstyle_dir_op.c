#include "filesysgrader.h"
#include "stdio.h"
#include "stdlib.h"

char buf[100];

int main(int argc, char** argv)
{
	int size = getFreeDiskSize();
	int dst=0;

	dst=creat("b");
	close(dst);
	assertTrueWMsg(mkdir("#b") == 0,"mkdir error #b");
	assertTrueWMsg(mkdir("@b") == 0,"mkdir error @b");
	assertTrueWMsg(mkdir("%b") == 0,"mkdir error %b");
	assertTrueWMsg(mkdir("^b") == 0,"mkdir error ^b");
	assertTrueWMsg(mkdir("&b") == 0,"mkdir error &b");
	assertTrueWMsg(mkdir("*b") == 0,"mkdir error *b");
	assertTrueWMsg(mkdir("_b") == 0,"mkdir error _b");
	assertTrueWMsg(mkdir("+b") == 0,"mkdir error +b");
	assertTrueWMsg(mkdir("=b") == 0,"mkdir error =b");
	assertTrueWMsg(mkdir("\\b") == 0,"mkdir error \\b");
	assertTrueWMsg(mkdir("?b") == 0,"mkdir error ?b");
	assertTrueWMsg(mkdir(".b") == 0,"mkdir error .b");
	assertTrueWMsg(mkdir(",b") == 0,"mkdir error ,b");
	assertTrueWMsg(mkdir("b") != 0,"mkdir b should failed");
	assertTrue(rmdir("#b") == 0);
	assertTrue(rmdir("@b") == 0);
	assertTrue(rmdir("%b") == 0);
	assertTrue(rmdir("^b") == 0);
	assertTrue(rmdir("&b") == 0);
	assertTrue(rmdir("*b") == 0);
	assertTrue(rmdir("_b") == 0);
	assertTrue(rmdir("+b") == 0);
	assertTrue(rmdir("=b") == 0);
	assertTrue(rmdir("\\b") == 0);
	assertTrue(rmdir("?b") == 0);
	assertTrue(rmdir(".b") == 0);
	assertTrue(rmdir(",b") == 0);
	assertTrue(unlink("b") == 0);


	assertTrue(getFreeDiskSize() == size);
	done();
	return 0;
}
