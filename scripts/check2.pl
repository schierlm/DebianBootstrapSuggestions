#!/usr/bin/perl
### (c) 2021 Michael Schierl
### Licensed under MIT License

use strict;
use warnings;
use AptPkg::Cache;
use AptPkg::Source;

my $source = AptPkg::Source->new();
my $cache = AptPkg::Cache->new();
my $packages = $cache->packages;

sub print_source_info {
	my ($f, @src) = ($_[0], @{$_[1]});
	my $size = 0;
	foreach my $file (@{$src[0]{'Files'}}) {
		$size += ${$file}{'Size'};
	}
	my $dst = "";
	foreach my $bin (sort @{$src[0]{'Binaries'}}) {
		my %pack = $packages->lookup($bin);
		next unless exists $pack{'Name'};
		$dst .= '+' if $dst ne '';
		$dst .= $pack{'Name'}.'('.$pack{'ShortDesc'}.')';
	}
	my $name = $src[0]{'Package'};
	print $f "$size\t$name\t$dst\n";
}

print "Pass 4\n";
open(my $in, "<", "tocheck1.txt") or die $!;
open(my $out1, ">", "_tocheck2.txt") or die $!;
open(my $out2, ">", "_directselfdeps.tsv") or die $!;
my $idx = 0;
my %binaries;

while(my $line = <$in>) {
	chomp $line;
	$idx++;
	if ($idx % 1000 == 0) {
		print "\t$idx\n";
	}
	my @src = $source->find($line, 1);
	%binaries = ();
	foreach my $bin (@{$src[0]{'Binaries'}}) {
		$binaries{$bin} = 1;
	}
	my $selfdep = 0;
	foreach my $depK (keys %{$src[0]{'BuildDepends'}}) {
		next if $depK !~ '^Build-D';
		foreach my $dep (@{$src[0]{'BuildDepends'}{$depK}}) {
			if (exists $binaries{${$dep}[0]}) {
				$selfdep = 1;
				last;
			}
		}
	}
	if ($selfdep) {
		&print_source_info($out2, \@src);
	} else {
		print $out1 "$line\n";
	}
}
close $in;
close $out1;
close $out2;

print "Done";
