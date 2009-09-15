#!/usr/perl

$fname = "game180.log";
$newfile = $fname.".log";

open(FILE, $fname);
open(NEWFILE, ">$newfile");
while (my $line = <FILE>) {
    #if ($line =~ /QuakTAC/ && $line =~ /CTR/) {
  if ($line =~ /Query \(lioneer,dvd\)/ && $line =~ /conversions yesterday/) {
    print NEWFILE $line;
  }
}
close(NEWFILE);
close(FILE);
