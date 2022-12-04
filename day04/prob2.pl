#!/usr/bin/env perl
use feature qw(say);

my $overlapped = 0;
while (<>) {
    my @pairs = split(',');
    my ($start1, $end1) = map(int, split('-', $pairs[0]));
    my ($start2, $end2) = map(int, split('-', $pairs[1]));
    if (!($start1 > $end2 || $end1 < $start2)) {
        $overlapped++;
    }
}
say $overlapped;
