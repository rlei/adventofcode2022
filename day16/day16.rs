use clap::Parser;
use itertools::Itertools;
use regex::Regex;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::{cmp, io};

type ValveId = u8;

#[derive(Debug)]
struct Valve {
    id: ValveId,
    rate: u16,
    edges: Vec<ValveId>,
}

#[derive(Parser, Debug)]
#[command(author, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value_t = 30)]
    minutes: u16,

    /// Solves problem 2
    #[arg(long, default_value_t = false)]
    prob2: bool,
}

fn main() {
    let args = Args::parse();
    let minutes = args.minutes;
    println!("Using minutes: {}", minutes);

    let re =
        Regex::new(r"Valve (\S+) has flow rate=(\d+); tunnels? leads? to valves? (.*)").unwrap();

    let valves: Vec<_> = io::stdin()
        .lines()
        .map(|line| {
            let l = line.unwrap();
            let cap = re.captures(l.as_str()).unwrap();
            let name = cap[1].to_string();
            let rate = cap[2].parse::<u16>().unwrap();
            let leads_to: Vec<_> = cap[3].split(", ").map(|s| s.to_owned()).collect();

            // let valve = Valve::new(name.clone(), rate, leads_to);
            println!("Valve: {} rate: {} leads to {:?}", name, rate, &leads_to);
            (name, rate, leads_to)
        })
        .collect();

    let valves_to_id: HashMap<String, ValveId> =
        valves.iter().map(|v| v.0.clone()).zip(0..).collect();
    // let id_to_valves: HashMap<_, _> = valves_to_id.iter().map(|(k, v)| (*v, k)).collect();

    let graph: HashMap<_, _> = valves
        .into_iter()
        .map(|valve| {
            let valve_id = Valve {
                id: valves_to_id[&valve.0],
                rate: valve.1,
                edges: valve.2.iter().map(|name| valves_to_id[name]).collect(),
            };
            (valve_id.id, valve_id)
        })
        .collect();

    let start_valve = valves_to_id["AA"];
    let good_valves: Vec<_> = graph
        .values()
        .filter(|valve| valve.rate > 0)
        .map(|v| v.id)
        .collect();
    let good_valves_distances = find_valves_distances(&graph, start_valve, &good_valves);

    if !args.prob2 {
        let max_pressure = find_max_pressure(
            &graph,
            start_valve,
            &good_valves,
            &good_valves_distances,
            minutes,
        );
        println!("Most pressure {:?}", max_pressure);
    } else {
        let max_pressure = find_max_pressure2(
            &graph,
            start_valve,
            &good_valves,
            &good_valves_distances,
            minutes,
        );
        println!("{:?}", max_pressure);
        println!(
            "Most pressure {}",
            max_pressure.0.released + max_pressure.1.released
        );
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
struct State {
    opened: Vec<ValveId>,
    released: u16,
    remaining_minutes: u16,
}

fn find_max_pressure(
    graph: &HashMap<ValveId, Valve>,
    start_valve: ValveId,
    good_valves: &Vec<ValveId>,
    good_valves_distances: &HashMap<(ValveId, ValveId), u16>,
    remaining_minutes: u16,
) -> (Vec<ValveId>, u16) {
    let mut visited_states = vec![State {
        opened: vec![start_valve],
        released: 0,
        remaining_minutes,
    }];

    let mut final_paths = Vec::new();

    loop {
        let mut next_visited_states = Vec::new();
        for state in visited_states.into_iter() {
            let mut next_moves = Vec::new();
            for v in good_valves.iter() {
                if !state.opened.contains(v) {
                    if let Some(next) = try_move(&state, *v, graph[v].rate, good_valves_distances) {
                        next_moves.push(next);
                        // println!("opened {:?}, {:?}", now_opened, state);
                    };
                }
            }
            if next_moves.is_empty() {
                // no more move for this state
                final_paths.push(state);
            } else {
                next_visited_states.extend(next_moves);
            }
        }
        if next_visited_states.is_empty() {
            // all possible paths (within the time bound) have been traversed
            break;
        }
        visited_states = next_visited_states;
    }
    let max_state = final_paths
        .into_iter()
        .max_by_key(|state| state.released)
        .unwrap();
    (max_state.opened, max_state.released)
}

fn find_max_pressure2(
    graph: &HashMap<ValveId, Valve>,
    start_valve: ValveId,
    good_valves: &Vec<ValveId>,
    good_valves_distances: &HashMap<(ValveId, ValveId), u16>,
    remaining_minutes: u16,
) -> (State, State) {
    let init = State {
        opened: vec![start_valve],
        released: 0,
        remaining_minutes,
    };
    let mut visited_states = HashSet::from([(init.clone(), init.clone())]);

    let mut max_path: Option<(State, State)> = None;
    let mut max_pressure = 0;

    loop {
        let mut next_visited_states = HashSet::new();
        let visited_states_len = visited_states.len();
        for (state1, state2) in visited_states.into_iter() {
            let mut next_moves = HashSet::new();

            let visited: HashSet<_> = state1.opened.iter().chain(state2.opened.iter()).collect();

            let next_valves: Vec<_> = good_valves
                .iter()
                .filter(|v| !visited.contains(v))
                .collect();

            if next_valves.len() > 1 {
                for pair in next_valves.into_iter().permutations(2) {
                    let next1 = try_move(
                        &state1,
                        *pair[0],
                        graph[pair[0]].rate,
                        good_valves_distances,
                    );
                    let next2 = try_move(
                        &state2,
                        *pair[1],
                        graph[pair[1]].rate,
                        good_valves_distances,
                    );
                    if next1.is_some() || next2.is_some() {
                        next_moves.insert((
                            next1.unwrap_or(state1.clone()),
                            next2.unwrap_or(state2.clone()),
                        ));
                    }
                }
            } else if next_valves.len() == 1 {
                let next = next_valves[0];
                if let Some(next1) =
                    try_move(&state1, *next, graph[next].rate, good_valves_distances)
                {
                    next_moves.insert((next1, state2.clone()));
                } else if let Some(next2) =
                    try_move(&state2, *next, graph[next].rate, good_valves_distances)
                {
                    next_moves.insert((state1.clone(), next2));
                };
            }
            /*
            println!(
                "{} next moves for state {:?}",
                next_moves.len(),
                (&state1, &state2)
            );
            */
            if next_moves.is_empty() {
                if state1.released + state2.released > max_pressure {
                    max_pressure = state1.released + state2.released;
                    max_path = Some((state1, state2));
                }
            } else {
                for (state1, state2) in next_moves {
                    if !next_visited_states.contains(&(state1.clone(), state2.clone()))
                        && !next_visited_states.contains(&(state2.clone(), state1.clone()))
                    {
                        next_visited_states.insert((state1, state2));
                    }
                }
            }
        }
        println!(
            "{} visited states, {} next states",
            visited_states_len,
            next_visited_states.len()
        );
        if next_visited_states.is_empty() {
            // all possible paths (within the time bound) have been traversed
            break;
        }
        visited_states = next_visited_states;
    }
    max_path.unwrap()
}

fn try_move(
    state: &State,
    next: ValveId,
    next_rate: u16,
    distances: &HashMap<(ValveId, ValveId), u16>,
) -> Option<State> {
    // +1 for opening the Valve
    let minutes_took = distances[&(*state.opened.last().unwrap(), next)] + 1;
    if state.remaining_minutes < minutes_took {
        return None;
    }
    let remaining = state.remaining_minutes - minutes_took;

    let mut now_opened = Vec::with_capacity(state.opened.len() + 1);
    now_opened.extend(state.opened.iter());
    now_opened.push(next);

    Some(State {
        opened: now_opened,
        released: state.released + next_rate * remaining,
        remaining_minutes: remaining,
    })
}

fn find_valves_distances(
    graph: &HashMap<ValveId, Valve>,
    start_valve: ValveId,
    good_valves: &Vec<ValveId>,
) -> HashMap<(ValveId, ValveId), u16> {
    let mut result = HashMap::new();
    good_valves.iter().for_each(|valve| {
        let distance = dijkstra_shortest_distance(graph, start_valve, *valve);
        result.insert((start_valve, *valve), distance);
        // println!("{} to {}: {}", start_valve, *valve, distance);
    });
    good_valves.iter().combinations(2).for_each(|pair| {
        let (from, to) = (pair[0], pair[1]);
        let distance = dijkstra_shortest_distance(graph, *from, *to);
        result.insert((*from, *to), distance);
        result.insert((*to, *from), distance);
        // println!("{} to {}: {}", from, to, distance);
    });
    result
}

fn dijkstra_shortest_distance(graph: &HashMap<ValveId, Valve>, from: ValveId, to: ValveId) -> u16 {
    let mut cost_map: HashMap<_, _> = graph.keys().map(|valve| (*valve, std::u16::MAX)).collect();
    let mut visited = HashSet::with_capacity(graph.len());
    let mut heap = BinaryHeap::new();

    cost_map.insert(from, 0);
    heap.push(cmp::Reverse((0, from)));
    while heap.len() > 0 {
        let cmp::Reverse((_cost, valve)) = heap.pop().unwrap();
        visited.insert(valve);

        for next in graph[&valve].edges.iter() {
            if !visited.contains(&next) {
                let old_cost = cost_map[next];
                let new_cost = cost_map[&valve] + 1;
                if new_cost < old_cost {
                    // got the shortest distance
                    if *next == to {
                        return new_cost;
                    }
                    heap.push(cmp::Reverse((new_cost, *next)));
                    cost_map.insert(*next, new_cost);
                }
            }
        }
    }

    // impossible
    std::u16::MAX
}
