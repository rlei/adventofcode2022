## Problem 1 of Day 2

Solved with [sed](https://en.wikipedia.org/wiki/Sed) and [bc](https://en.wikipedia.org/wiki/Bc_%28programming_language%29),
thus no source file other than the shell command below:

```lang=shell
(sed '
s/A X/score+=3+1/g;
s/A Y/score+=6+2/g;
s/A Z/score+=0+3/g;
s/B X/score+=0+1/g;
s/B Y/score+=3+2/g;
s/B Z/score+=6+3/g;
s/C X/score+=6+1/g;
s/C Y/score+=0+2/g;
s/C Z/score+=3+3/g;
' path/to/day2_input; echo 'score' ) | bc
```


## Problem 2 of Day 2

```lang=shell
(sed '
s/A X/score+=0+3/g;
s/A Y/score+=3+1/g;
s/A Z/score+=6+2/g;
s/B X/score+=0+1/g;
s/B Y/score+=3+2/g;
s/B Z/score+=6+3/g;
s/C X/score+=0+2/g;
s/C Y/score+=3+3/g;
s/C Z/score+=6+1/g;
' path/to/day2_input; echo 'score' ) | bc
```

