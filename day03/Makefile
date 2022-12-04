LDFLAGS=-lSystem -syslibroot `xcrun -sdk macosx --show-sdk-path` -e _start -arch arm64 

all: prob1 prob2

%: %.o
	ld -o $@ $< $(LDFLAGS)

%.o: %.s common.s
	as -arch arm64 -o $@ $<
