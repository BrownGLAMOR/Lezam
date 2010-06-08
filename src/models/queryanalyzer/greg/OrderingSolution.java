package models.queryanalyzer.greg;


import java.util.ArrayList;

public class OrderingSolution {
	int[] _ordering;
	int _solutions;
	ArrayList<int[][]> _waterfall;
	
	public OrderingSolution(int solutions,ArrayList<int[][]> waterfall,int[] ordering){
		_solutions = solutions;
		_waterfall = waterfall;
		_ordering = ordering;
	}
	
}
