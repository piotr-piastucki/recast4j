/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
Recast4J Copyright (c) 2015 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.crowd;

import static org.recast4j.detour.DetourCommon.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.recast4j.detour.ClosesPointOnPolyResult;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.FindPathResult;
import org.recast4j.detour.IQueryFilter;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.QueryFilter;
import org.recast4j.detour.Status;
import org.recast4j.detour.Tupple2;
import org.recast4j.detour.crowd.CrowdAgent.CrowdAgentState;
import org.recast4j.detour.crowd.CrowdAgent.MoveRequestState;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.crowd.debug.CrowdAgentDebugInfo;
import org.recast4j.detour.crowd.debug.ObstacleAvoidanceDebugData;

/*

struct dtCrowdAgentDebugInfo
{
	int idx;
	float optStart[3], optEnd[3];
	dtObstacleAvoidanceDebugData* vod;
};

/// Provides local steering behaviors for a group of agents. 
/// @ingroup crowd
class dtCrowd
{
	int m_maxAgents;
	dtCrowdAgent* m_agents;
	dtCrowdAgent** m_activeAgents;
	dtCrowdAgentAnimation* m_agentAnims;
	
	dtPathQueue m_pathq;

	dtObstacleAvoidanceParams m_obstacleQueryParams[DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS];
	dtObstacleAvoidanceQuery* m_obstacleQuery;
	
	dtPolyRef* m_pathResult;
	int m_maxPathResult;
	
	float m_ext[3];

	dtQueryFilter m_filters[DT_CROWD_MAX_QUERY_FILTER_TYPE];

	float m_maxAgentRadius;

	int m_velocitySampleCount;

	dtNavMeshQuery* m_navquery;

	void updateTopologyOptimization(dtCrowdAgent** agents, int nagents, float dt);
	void updateMoveRequest(float dt);
	void checkPathValidity(dtCrowdAgent** agents, int nagents, float dt);

	inline int getAgentIndex(dtCrowdAgent* agent)  { return (int)(agent - m_agents); }

	bool requestMoveTargetReplan(int idx, dtPolyRef ref, float* pos);

	void purge();
	
public:
	
	/// Gets the specified agent from the pool.
	///	 @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return The requested agent.
	dtCrowdAgent* getAgent(int idx);

	/// Gets the specified agent from the pool.
	///	 @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return The requested agent.
	dtCrowdAgent* getEditableAgent(int idx);

	/// The maximum number of agents that can be managed by the object.
	/// @return The maximum number of agents.
	int getAgentCount() const;
	
	/// Adds a new agent to the crowd.
	///  @param[in]		pos		The requested position of the agent. [(x, y, z)]
	///  @param[in]		params	The configutation of the agent.
	/// @return The index of the agent in the agent pool. Or -1 if the agent could not be added.
	int addAgent(float* pos, dtCrowdAgentParams* params);

	/// Updates the specified agent's configuration.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	///  @param[in]		params	The new agent configuration.
	void updateAgentParameters(int idx, dtCrowdAgentParams* params);

	/// Removes the agent from the crowd.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	void removeAgent(int idx);
	
	/// Submits a new move request for the specified agent.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	///  @param[in]		ref		The position's polygon reference.
	///  @param[in]		pos		The position within the polygon. [(x, y, z)]
	/// @return True if the request was successfully submitted.
	bool requestMoveTarget(int idx, dtPolyRef ref, float* pos);

	/// Submits a new move request for the specified agent.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	///  @param[in]		vel		The movement velocity. [(x, y, z)]
	/// @return True if the request was successfully submitted.
	bool requestMoveVelocity(int idx, float* vel);

	/// Resets any request for the specified agent.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return True if the request was successfully reseted.
	bool resetMoveTarget(int idx);

	/// Gets the active agents int the agent pool.
	///  @param[out]	agents		An array of agent pointers. [(#dtCrowdAgent *) * maxAgents]
	///  @param[in]		maxAgents	The size of the crowd agent array.
	/// @return The number of agents returned in @p agents.
	int getActiveAgents(dtCrowdAgent** agents, int maxAgents);

	/// Updates the steering and positions of all agents.
	///  @param[in]		dt		The time, in seconds, to update the simulation. [Limit: > 0]
	///  @param[out]	debug	A debug object to load with debug information. [Opt]
	void update(float dt, dtCrowdAgentDebugInfo* debug);
	
	/// Gets the filter used by the crowd.
	/// @return The filter used by the crowd.
	inline dtQueryFilter* getFilter(int i) { return (i >= 0 && i < DT_CROWD_MAX_QUERY_FILTER_TYPE) ? &m_filters[i] : 0; }
	
	/// Gets the filter used by the crowd.
	/// @return The filter used by the crowd.
	inline dtQueryFilter* getEditableFilter(int i) { return (i >= 0 && i < DT_CROWD_MAX_QUERY_FILTER_TYPE) ? &m_filters[i] : 0; }

	/// Gets the search extents [(x, y, z)] used by the crowd for query operations. 
	/// @return The search extents used by the crowd. [(x, y, z)]
	float* getQueryExtents() { return m_ext; }
	
	/// Gets the velocity sample count.
	/// @return The velocity sample count.
	inline int getVelocitySampleCount() { return m_velocitySampleCount; }
	
	/// Gets the crowd's proximity grid.
	/// @return The crowd's proximity grid.
	dtProximityGrid* getGrid() { return m_grid; }

	/// Gets the crowd's path request queue.
	/// @return The crowd's path request queue.
	dtPathQueue* getPathQueue() { return &m_pathq; }

	/// Gets the query object used by the crowd.
	dtNavMeshQuery* getNavMeshQuery() { return m_navquery; }
};

/// Allocates a crowd object using the Detour allocator.
/// @return A crowd object that is ready for initialization, or null on failure.
///  @ingroup crowd
dtCrowd* dtAllocCrowd();

/// Frees the specified crowd object using the Detour allocator.
///  @param[in]		ptr		A crowd object allocated using #dtAllocCrowd
///  @ingroup crowd
void dtFreeCrowd(dtCrowd* ptr);

*/
/**
 * Members in this module implement local steering and dynamic avoidance features.
 * 
 * The crowd is the big beast of the navigation features. It not only handles a lot of the path management for you, but
 * also local steering and dynamic avoidance between members of the crowd. I.e. It can keep your agents from running
 * into each other.
 * 
 * Main class: Crowd
 * 
 * The #dtNavMeshQuery and #dtPathCorridor classes provide perfectly good, easy to use path planning features. But in
 * the end they only give you points that your navigation client should be moving toward. When it comes to deciding
 * things like agent velocity and steering to avoid other agents, that is up to you to implement. Unless, of course, you
 * decide to use Crowd.
 * 
 * Basically, you add an agent to the crowd, providing various configuration settings such as maximum speed and
 * acceleration. You also provide a local target to move toward. The crowd manager then provides, with every update, the
 * new agent position and velocity for the frame. The movement will be constrained to the navigation mesh, and steering
 * will be applied to ensure agents managed by the crowd do not collide with each other.
 * 
 * This is very powerful feature set. But it comes with limitations.
 * 
 * The biggest limitation is that you must give control of the agent's position completely over to the crowd manager.
 * You can update things like maximum speed and acceleration. But in order for the crowd manager to do its thing, it
 * can't allow you to constantly be giving it overrides to position and velocity. So you give up direct control of the
 * agent's movement. It belongs to the crowd.
 * 
 * The second biggest limitation revolves around the fact that the crowd manager deals with local planning. So the
 * agent's target should never be more than 256 polygons away from its current position. If it is, you risk your agent
 * failing to reach its target. So you may still need to do long distance planning and provide the crowd manager with
 * intermediate targets.
 * 
 * Other significant limitations:
 * 
 * - All agents using the crowd manager will use the same #dtQueryFilter. - Crowd management is relatively expensive.
 * The maximum agents under crowd management at any one time is between 20 and 30. A good place to start is a maximum of
 * 25 agents for 0.5ms per frame.
 * 
 * @note This is a summary list of members. Use the index or search feature to find minor members.
 * 
 * @struct dtCrowdAgentParams
 * @see CrowdAgent, Crowd::addAgent(), Crowd::updateAgentParameters()
 * 
 * @var dtCrowdAgentParams::obstacleAvoidanceType
 * @par
 * 
 * 		#dtCrowd permits agents to use different avoidance configurations. This value is the index of the
 *      #dtObstacleAvoidanceParams within the crowd.
 * 
 * @see dtObstacleAvoidanceParams, dtCrowd::setObstacleAvoidanceParams(), dtCrowd::getObstacleAvoidanceParams()
 * 
 * @var dtCrowdAgentParams::collisionQueryRange
 * @par
 * 
 * 		Collision elements include other agents and navigation mesh boundaries.
 * 
 *      This value is often based on the agent radius and/or maximum speed. E.g. radius * 8
 * 
 * @var dtCrowdAgentParams::pathOptimizationRange
 * @par
 * 
 * 		Only applicalbe if #updateFlags includes the #DT_CROWD_OPTIMIZE_VIS flag.
 * 
 *      This value is often based on the agent radius. E.g. radius * 30
 * 
 * @see dtPathCorridor::optimizePathVisibility()
 * 
 * @var dtCrowdAgentParams::separationWeight
 * @par
 * 
 * 		A higher value will result in agents trying to stay farther away from each other at the cost of more difficult
 *      steering in tight spaces.
 *
 */
/**
This is the core class of the @ref crowd module.  See the @ref crowd documentation for a summary
of the crowd features.
A common method for setting up the crowd is as follows:
-# Allocate the crowd
-# Set the avoidance configurations using #setObstacleAvoidanceParams().
-# Add agents using #addAgent() and make an initial movement request using #requestMoveTarget().
A common process for managing the crowd is as follows:
-# Call #update() to allow the crowd to manage its agents.
-# Retrieve agent information using #getActiveAgents().
-# Make movement requests using #requestMoveTarget() when movement goal changes.
-# Repeat every frame.
Some agent configuration settings can be updated using #updateAgentParameters().  But the crowd owns the
agent position.  So it is not possible to update an active agent's position.  If agent position
must be fed back into the crowd, the agent must be removed and re-added.
Notes: 
- Path related information is available for newly added agents only after an #update() has been
  performed.
- Agent objects are kept in a pool and re-used.  So it is important when using agent objects to check the value of
  #dtCrowdAgent::active to determine if the agent is actually in use or not.
- This class is meant to provide 'local' movement. There is a limit of 256 polygons in the path corridor.  
  So it is not meant to provide automatic pathfinding services over long distances.
@see dtAllocCrowd(), dtFreeCrowd(), init(), dtCrowdAgent
*/
public class Crowd {

	static final int MAX_ITERS_PER_UPDATE = 100;

	static final int MAX_PATHQUEUE_NODES = 4096;
	static final int MAX_COMMON_NODES = 512;

	/// The maximum number of neighbors that a crowd agent can take into account
	/// for steering decisions.
	/// @ingroup crowd
	static final int DT_CROWDAGENT_MAX_NEIGHBOURS = 6;

	/// The maximum number of corners a crowd agent will look ahead in the path.
	/// This value is used for sizing the crowd agent corner buffers.
	/// Due to the behavior of the crowd manager, the actual number of useful
	/// corners will be one less than this number.
	/// @ingroup crowd
	static final int DT_CROWDAGENT_MAX_CORNERS = 4;

	/// The maximum number of crowd avoidance configurations supported by the
	/// crowd manager.
	/// @ingroup crowd
	/// @see dtObstacleAvoidanceParams, dtCrowd::setObstacleAvoidanceParams(), dtCrowd::getObstacleAvoidanceParams(),
	///		 dtCrowdAgentParams::obstacleAvoidanceType
	static final int DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS = 8;

	/// The maximum number of query filter types supported by the crowd manager.
	/// @ingroup crowd
	/// @see dtQueryFilter, dtCrowd::getFilter() dtCrowd::getEditableFilter(),
	///		dtCrowdAgentParams::queryFilterType
	static final int DT_CROWD_MAX_QUERY_FILTER_TYPE = 16;

	/// Provides neighbor data for agents managed by the crowd.
	/// @ingroup crowd
	/// @see dtCrowdAgent::neis, dtCrowd
	class CrowdNeighbour
	{
		final int idx;		///< The index of the neighbor in the crowd.
		final float dist;		///< The distance between the current agent and the neighbor.
		public CrowdNeighbour(int idx, float dist) {
			this.idx = idx;
			this.dist = dist;
		}
	};


	int m_maxAgents;
	CrowdAgent[] m_agents;
	List<CrowdAgent> m_activeAgents;
	PathQueue m_pathq;

	ObstacleAvoidanceParams[] m_obstacleQueryParams = new ObstacleAvoidanceParams[DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS];
	ObstacleAvoidanceQuery m_obstacleQuery;
	
	ProximityGrid m_grid;
	
	float[] m_ext = new float[3];

	IQueryFilter[] m_filters = new IQueryFilter[DT_CROWD_MAX_QUERY_FILTER_TYPE];

	float m_maxAgentRadius;

	int m_velocitySampleCount;

	NavMeshQuery m_navquery;

	float tween(float t, float t0, float t1) {
		return clamp((t - t0) / (t1 - t0), 0.0f, 1.0f);
	}

	List<CrowdNeighbour> getNeighbours(float[] pos, float height, float range, CrowdAgent skip,
			List<CrowdAgent> agents, ProximityGrid grid) {

		List<CrowdNeighbour> result = new ArrayList<>();
		Set<Integer> ids = grid.queryItems(pos[0] - range, pos[2] - range, pos[0] + range, pos[2] + range);

		for (int id : ids) {
			CrowdAgent ag = agents.get(id);

			if (ag == skip)
				continue;

			// Check for overlap.
			float[] diff = vSub(pos, ag.npos);
			if (Math.abs(diff[1]) >= (height + ag.params.height) / 2.0f)
				continue;
			diff[1] = 0;
			float distSqr = vLenSqr(diff);
			if (distSqr > sqr(range))
				continue;

			addNeighbour(id, distSqr, result);
		}
		return result;

	}

	void addNeighbour(int idx, float dist, List<CrowdNeighbour> neis) {
		// Insert neighbour based on the distance.
		CrowdNeighbour nei = new CrowdNeighbour(idx, dist);
		neis.add(nei);
		Collections.sort(neis, (o1, o2) -> Float.compare(o1.dist, o2.dist));
	}

	public void addToOptQueue(CrowdAgent newag, PriorityQueue<CrowdAgent> agents) {
		// Insert neighbour based on greatest time.
		agents.add(newag);
	}

	// Insert neighbour based on greatest time.
	void addToPathQueue(CrowdAgent newag, PriorityQueue<CrowdAgent> agents) {
		agents.add(newag);
	}

	///
	/// Initializes the crowd.  
	/// May be called more than once to purge and re-initialize the crowd.
	///  @param[in]		maxAgents		The maximum number of agents the crowd can manage. [Limit: >= 1]
	///  @param[in]		maxAgentRadius	The maximum radius of any agent that will be added to the crowd. [Limit: > 0]
	///  @param[in]		nav				The navigation mesh to use for planning.
	/// @return True if the initialization succeeded.
	public Crowd(int maxAgents, float maxAgentRadius, NavMesh nav) {

		m_maxAgents = maxAgents;
		m_maxAgentRadius = maxAgentRadius;
		vSet(m_ext, m_maxAgentRadius * 2.0f, m_maxAgentRadius * 1.5f, m_maxAgentRadius * 2.0f);

		m_grid = new ProximityGrid(m_maxAgents * 4, maxAgentRadius * 3);
		m_obstacleQuery = new ObstacleAvoidanceQuery(6, 8);

		for (int i = 0; i < DT_CROWD_MAX_QUERY_FILTER_TYPE; i++) {
			m_filters[i] = new QueryFilter();
		}
		// Init obstacle query params.
		for (int i = 0; i < DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS; ++i) {
			m_obstacleQueryParams[i] = new ObstacleAvoidanceParams();
		}

		// Allocate temp buffer for merging paths.
		m_pathq = new PathQueue(MAX_PATHQUEUE_NODES, nav);
		m_agents = new CrowdAgent[m_maxAgents];
		m_activeAgents = new ArrayList<>();
		for (int i = 0; i < m_maxAgents; ++i) {
			m_agents[i] = new CrowdAgent(i);
			m_agents[i].active = false;
		}

		// The navquery is mostly used for local searches, no need for large
		// node pool.
		m_navquery = new NavMeshQuery(nav);
	}

	/// Sets the shared avoidance configuration for the specified index.
	/// @param[in] idx The index. [Limits: 0 <= value <
	/// #DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS]
	/// @param[in] params The new configuration.
	public void setObstacleAvoidanceParams(int idx, ObstacleAvoidanceParams params) {
		if (idx >= 0 && idx < DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS) {
			m_obstacleQueryParams[idx] = params;
		}
	}

	/// Gets the shared avoidance configuration for the specified index.
	/// @param[in] idx The index of the configuration to retreive.
	/// [Limits: 0 <= value < #DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS]
	/// @return The requested configuration.
	public ObstacleAvoidanceParams getObstacleAvoidanceParams(int idx) {
		if (idx >= 0 && idx < DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS)
			return m_obstacleQueryParams[idx];
		return null;
	}

	/// The maximum number of agents that can be managed by the object.
	/// @return The maximum number of agents.
	public int getAgentCount() {
		return m_maxAgents;
	}

	/// Gets the specified agent from the pool.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return The requested agent.
	/// Agents in the pool may not be in use.  Check #dtCrowdAgent.active before using the returned object.
	public CrowdAgent getAgent(int idx) {
		return idx < 0 || idx >= m_agents.length ? null : m_agents[idx];
	}

	/// 
	/// Gets the specified agent from the pool.
	///	 @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return The requested agent.
	/// Agents in the pool may not be in use.  Check #dtCrowdAgent.active before using the returned object.
	public CrowdAgent getEditableAgent(int idx) {
		return idx < 0 || idx >= m_agents.length ? null : m_agents[idx];
	}

	/// Updates the specified agent's configuration.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @param[in] params The new agent configuration.
	public void updateAgentParameters(int idx, CrowdAgentParams params) {
		if (idx < 0 || idx >= m_maxAgents)
			return;
		m_agents[idx].params = params;
	}

	/// Adds a new agent to the crowd.
	/// @param[in] pos The requested position of the agent. [(x, y, z)]
	/// @param[in] params The configutation of the agent.
	/// @return The index of the agent in the agent pool. Or -1 if the agent
	/// could not be added.
	public int addAgent(float[] pos, CrowdAgentParams params) {
		// Find empty slot.
		int idx = -1;
		for (int i = 0; i < m_maxAgents; ++i) {
			if (!m_agents[i].active) {
				idx = i;
				break;
			}
		}
		if (idx == -1)
			return -1;

		CrowdAgent ag = m_agents[idx];

		updateAgentParameters(idx, params);

		// Find nearest position on navmesh and place the agent there.
		FindNearestPolyResult nearest = m_navquery.findNearestPoly(pos, m_ext, m_filters[ag.params.queryFilterType]);

		ag.corridor.reset(nearest.getNearestRef(), nearest.getNearestPos());
		ag.boundary.reset();
		ag.partial = false;

		ag.topologyOptTime = 0;
		ag.targetReplanTime = 0;

		vSet(ag.dvel, 0, 0, 0);
		vSet(ag.nvel, 0, 0, 0);
		vSet(ag.vel, 0, 0, 0);
		vCopy(ag.npos, nearest.getNearestPos());

		ag.desiredSpeed = 0;

		if (nearest.getNearestRef() != 0)
			ag.state = CrowdAgentState.DT_CROWDAGENT_STATE_WALKING;
		else
			ag.state = CrowdAgentState.DT_CROWDAGENT_STATE_INVALID;

		ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_NONE;

		ag.active = true;

		return idx;
	}


	/// Removes the agent from the crowd.
	///  @param[in]		idx		The agent index. [Limits: 0 <= value < #getAgentCount()]
	///
	/// The agent is deactivated and will no longer be processed. Its
	/// #dtCrowdAgent object
	/// is not removed from the pool. It is marked as inactive so that it is
	/// available for reuse.
	/// Removes the agent from the crowd.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	public void removeAgent(int idx) {
		if (idx >= 0 && idx < m_maxAgents) {
			m_agents[idx].active = false;
		}
	}

	boolean requestMoveTargetReplan(CrowdAgent ag, long ref, float[] pos) {
		ag.setTarget(ref, pos);
		ag.targetReplan = true;
		return true;
	}


	/// Submits a new move request for the specified agent.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @param[in] ref The position's polygon reference.
	/// @param[in] pos The position within the polygon. [(x, y, z)]
	/// @return True if the request was successfully submitted.
	/// 
	/// This method is used when a new target is set.
	/// 
	/// The position will be constrained to the surface of the navigation mesh.
	///
	/// The request will be processed during the next #update().
	public boolean requestMoveTarget(int idx, long ref, float[] pos) {
		if (idx < 0 || idx >= m_maxAgents)
			return false;
		if (ref == 0)
			return false;

		CrowdAgent ag = m_agents[idx];

		// Initialize request.
		ag.setTarget(ref, pos);
		ag.targetReplan = false;

		return true;
	}

	/// Submits a new move request for the specified agent.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @param[in] vel The movement velocity. [(x, y, z)]
	/// @return True if the request was successfully submitted.
	public boolean requestMoveVelocity(int idx, float[] vel) {
		if (idx < 0 || idx >= m_maxAgents)
			return false;

		CrowdAgent ag = m_agents[idx];

		// Initialize request.
		ag.targetRef = 0;
		vCopy(ag.targetPos, vel);
		ag.targetPathqRef = PathQueue.DT_PATHQ_INVALID;
		ag.targetReplan = false;
		ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY;

		return true;
	}

	/// Resets any request for the specified agent.
	/// @param[in] idx The agent index. [Limits: 0 <= value < #getAgentCount()]
	/// @return True if the request was successfully reseted.
	public boolean resetMoveTarget(int idx) {
		if (idx < 0 || idx >= m_maxAgents)
			return false;

		CrowdAgent ag = m_agents[idx];

		// Initialize request.
		ag.targetRef = 0;
		vSet(ag.targetPos, 0, 0, 0);
		vSet(ag.dvel, 0, 0, 0);
		ag.targetPathqRef = PathQueue.DT_PATHQ_INVALID;
		ag.targetReplan = false;
		ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_NONE;
		return true;
	}

	/// Gets the active agents int the agent pool.
	///  @param[out]	agents		An array of agent pointers. [(#dtCrowdAgent *) * maxAgents]
	///  @param[in]		maxAgents	The size of the crowd agent array.
	/// @return The number of agents returned in @p agents.
	public List<CrowdAgent> getActiveAgents() {
		List<CrowdAgent> agents = new ArrayList<>(m_maxAgents);
		for (int i = 0; i < m_maxAgents; ++i) {
			if (m_agents[i].active) {
				agents.add(m_agents[i]);
			}
		}
		return agents;
	}

	static final int MAX_ITER = 20;

	void updateMoveRequest() {
		PriorityQueue<CrowdAgent> queue = new PriorityQueue<>(
				(a1, a2) -> Float.compare(a2.targetReplanTime, a1.targetReplanTime));

		// Fire off new requests.
		for (int i = 0; i < m_maxAgents; ++i) {
			CrowdAgent ag = m_agents[i];
			if (!ag.active)
				continue;
			if (ag.state == CrowdAgentState.DT_CROWDAGENT_STATE_INVALID)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE
					|| ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;

			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_REQUESTING) {
				List<Long> path = ag.corridor.getPath();
				if (path.isEmpty()) {
					throw new IllegalArgumentException("Empty path");
				}
				// Quick search towards the goal.
				m_navquery.initSlicedFindPath(path.get(0), ag.targetRef, ag.npos, ag.targetPos,
						m_filters[ag.params.queryFilterType], 0);
				m_navquery.updateSlicedFindPath(MAX_ITER);
				FindPathResult pathFound;
				if (ag.targetReplan) // && npath > 10)
				{
					// Try to use existing steady path during replan if
					// possible.
					pathFound = m_navquery.finalizeSlicedFindPathPartial(path);
				} else {
					// Try to move towards target when goal changes.
					pathFound = m_navquery.finalizeSlicedFindPath();
				}
				List<Long> reqPath = pathFound.getRefs();
				float[] reqPos = new float[3];
				if (!pathFound.getStatus().isFailed() && reqPath.size() > 0) {
					// In progress or succeed.
					if (reqPath.get(reqPath.size() - 1) != ag.targetRef) {
						// Partial path, constrain target position inside the
						// last polygon.
						ClosesPointOnPolyResult cr = m_navquery.closestPointOnPoly(reqPath.get(reqPath.size() - 1),
								ag.targetPos);
						reqPos = cr.getClosest();
					} else {
						vCopy(reqPos, ag.targetPos);
					}
				} else {
					// Could not find path, start the request from current
					// location.
					vCopy(reqPos, ag.npos);
					reqPath = new ArrayList<>();
					reqPath.add(path.get(0));
				}

				ag.corridor.setCorridor(reqPos, reqPath);
				ag.boundary.reset();
				ag.partial = false;

				if (reqPath.get(reqPath.size() - 1) == ag.targetRef) {
					ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_VALID;
					ag.targetReplanTime = 0.0f;
				} else {
					// The path is longer or potentially unreachable, full plan.
					ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_WAITING_FOR_QUEUE;
				}
			}

			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_WAITING_FOR_QUEUE) {
				addToPathQueue(ag, queue);
			}
		}

		while (!queue.isEmpty()) {
			CrowdAgent ag = queue.poll();
			ag.targetPathqRef = m_pathq.request(ag.corridor.getLastPoly(), ag.targetRef, ag.corridor.getTarget(),
					ag.targetPos, m_filters[ag.params.queryFilterType]);
			if (ag.targetPathqRef != PathQueue.DT_PATHQ_INVALID)
				ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_WAITING_FOR_PATH;
		}

		// Update requests.
		m_pathq.update(MAX_ITERS_PER_UPDATE);

		// Process path results.
		for (int i = 0; i < m_maxAgents; ++i) {
			CrowdAgent ag = m_agents[i];
			if (!ag.active)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE
					|| ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;

			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_WAITING_FOR_PATH) {
				// Poll path queue.
				Status status = m_pathq.getRequestStatus(ag.targetPathqRef);
				if (status != null && status.isFailed()) {
					// Path find failed, retry if the target location is still
					// valid.
					ag.targetPathqRef = PathQueue.DT_PATHQ_INVALID;
					if (ag.targetRef != 0)
						ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_REQUESTING;
					else
						ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_FAILED;
					ag.targetReplanTime = 0.0f;
				} else if (status != null && status.isSuccess()) {
					List<Long> path = ag.corridor.getPath();
					if (path.isEmpty()) {
						throw new IllegalArgumentException("Empty path");
					}

					// Apply results.
					float[] targetPos = ag.targetPos;

					boolean valid = true;
					FindPathResult pathFound = m_pathq.getPathResult(ag.targetPathqRef);
					List<Long> res = pathFound.getRefs();
					status = pathFound.getStatus();
					if (status.isFailed() || res.isEmpty())
						valid = false;

					if (status.isPartial())
						ag.partial = true;
					else
						ag.partial = false;

					// Merge result and existing path.
					// The agent might have moved whilst the request is
					// being processed, so the path may have changed.
					// We assume that the end of the path is at the same
					// location
					// where the request was issued.

					// The last ref in the old path should be the same as
					// the location where the request was issued..
					if (valid && path.get(path.size() - 1).longValue() != res.get(0).longValue())
						valid = false;

					if (valid) {
						// Put the old path infront of the old path.
						if (path.size() > 1) {
							path.remove(path.size() - 1);
							path.addAll(res);
							res = path;
							// Remove trackbacks
							for (int j = 1; j < res.size() - 1; ++j) {
								if (j - 1 >= 0 && j + 1 < res.size()) {
									if (res.get(j - 1).longValue() == res.get(j + 1).longValue()) {
										res.remove(j + 1);
										res.remove(j);
										j -= 2;
									}
								}
							}
						}

						// Check for partial path.
						if (res.get(res.size() - 1) != ag.targetRef) {
							// Partial path, constrain target position inside
							// the last polygon.
							ClosesPointOnPolyResult cr = m_navquery.closestPointOnPoly(res.get(res.size() - 1),
									targetPos);
							targetPos = cr.getClosest();
						}
					}

					if (valid) {
						// Set current corridor.
						ag.corridor.setCorridor(targetPos, res);
						// Force to update boundary.
						ag.boundary.reset();
						ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_VALID;
					} else {
						// Something went wrong.
						ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_FAILED;
					}

					ag.targetReplanTime = 0.0f;
				}
			}
		}
	}

	static final float OPT_TIME_THR = 0.5f; // seconds
	
	void updateTopologyOptimization(List<CrowdAgent> agents, float dt)
	{
		if (!agents.isEmpty())
			return;
		
		PriorityQueue<CrowdAgent> queue = new PriorityQueue<>(
				(a1, a2) -> Float.compare(a2.topologyOptTime, a1.topologyOptTime));
		
		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE || ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;
			if ((ag.params.updateFlags & CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO) == 0)
				continue;
			ag.topologyOptTime += dt;
			if (ag.topologyOptTime >= OPT_TIME_THR)
				addToOptQueue(ag, queue);
		}

		while (!queue.isEmpty())
		{
			CrowdAgent ag = queue.poll();
			ag.corridor.optimizePathTopology(m_navquery, m_filters[ag.params.queryFilterType]);
			ag.topologyOptTime = 0;
		}

	}

	static final int CHECK_LOOKAHEAD = 10;
	static final float TARGET_REPLAN_DELAY = 1.0f; // seconds

	void checkPathValidity(List<CrowdAgent> agents, float dt) {

		for (int i = 0; i < agents.size(); ++i) {
			CrowdAgent ag = agents.get(i);

			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;

			ag.targetReplanTime += dt;

			boolean replan = false;
			
			// First check that the current location is valid.
			float[] agentPos = new float[3];
			long agentRef = ag.corridor.getFirstPoly();
			vCopy(agentPos, ag.npos);
			if (!m_navquery.isValidPolyRef(agentRef, m_filters[ag.params.queryFilterType])) {
				// Current location is not valid, try to reposition.
				// TODO: this can snap agents, how to handle that?
				FindNearestPolyResult fnp = m_navquery.findNearestPoly(ag.npos, m_ext,
						m_filters[ag.params.queryFilterType]);
				agentRef = fnp.getNearestRef();
				if (fnp.getNearestPos() != null) {
					vCopy(agentPos, fnp.getNearestPos());
				}

				if (agentRef == 0) {
					// Could not find location in navmesh, set state to invalid.
					ag.corridor.reset(0, agentPos);
					ag.partial = false;
					ag.boundary.reset();
					ag.state = CrowdAgentState.DT_CROWDAGENT_STATE_INVALID;
					continue;
				}

				// Make sure the first polygon is valid, but leave other valid
				// polygons in the path so that replanner can adjust the path
				// better.
				ag.corridor.fixPathStart(agentRef, agentPos);
				// ag.corridor.trimInvalidPath(agentRef, agentPos, m_navquery,
				// &m_filter);
				ag.boundary.reset();
				vCopy(ag.npos, agentPos);

				replan = true;
			}

			// If the agent does not have move target or is controlled by
			// velocity, no need to recover the target nor replan.
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE
					|| ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;

			// Try to recover move request position.
			if (ag.targetState != MoveRequestState.DT_CROWDAGENT_TARGET_NONE
					&& ag.targetState != MoveRequestState.DT_CROWDAGENT_TARGET_FAILED) {
				if (!m_navquery.isValidPolyRef(ag.targetRef, m_filters[ag.params.queryFilterType])) {
					// Current target is not valid, try to reposition.
					FindNearestPolyResult fnp = m_navquery.findNearestPoly(ag.targetPos, m_ext,
							m_filters[ag.params.queryFilterType]);
					ag.targetRef = fnp.getNearestRef();
					if (fnp.getNearestPos() != null) {
						vCopy(ag.targetPos, fnp.getNearestPos());
					}
					replan = true;
				}
				if (ag.targetRef == 0) {
					// Failed to reposition target, fail moverequest.
					ag.corridor.reset(agentRef, agentPos);
					ag.partial = false;
					ag.targetState = MoveRequestState.DT_CROWDAGENT_TARGET_NONE;
				}
			}

			// If nearby corridor is not valid, replan.
			if (!ag.corridor.isValid(CHECK_LOOKAHEAD, m_navquery, m_filters[ag.params.queryFilterType])) {
				// Fix current path.
				// ag.corridor.trimInvalidPath(agentRef, agentPos, m_navquery,
				// &m_filter);
				// ag.boundary.reset();
				replan = true;
			}

			// If the end of the path is near and it is not the requested
			// location, replan.
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VALID) {
				if (ag.targetReplanTime > TARGET_REPLAN_DELAY && ag.corridor.getPathCount() < CHECK_LOOKAHEAD
						&& ag.corridor.getLastPoly() != ag.targetRef)
					replan = true;
			}

			// Try to replan path to goal.
			if (replan) {
				if (ag.targetState != MoveRequestState.DT_CROWDAGENT_TARGET_NONE) {
					requestMoveTargetReplan(ag, ag.targetRef, ag.targetPos);
				}
			}
		}
	}
	
	static final float COLLISION_RESOLVE_FACTOR = 0.7f;
	
	public void update(float dt, CrowdAgentDebugInfo debug)
	{
		m_velocitySampleCount = 0;
		
		int debugIdx = debug != null ? debug.idx : -1;
		
		List<CrowdAgent> agents = getActiveAgents();

		// Check that all agents still have valid paths.
		checkPathValidity(agents, dt);
		
		// Update async move request and path finder.
		updateMoveRequest();

		// Optimize path topology.
		updateTopologyOptimization(agents, dt);
		
		// Register agents to proximity grid.
		m_grid.clear();
		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			float[] p = ag.npos;
			float r = ag.params.radius;
			m_grid.addItem(i, p[0]-r, p[2]-r, p[0]+r, p[2]+r);
		}
		
		// Get nearby navmesh segments and agents to collide with.
		for (CrowdAgent ag : agents)
		{
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;

			// Update the collision boundary after certain distance has been passed or
			// if it has become invalid.
			float updateThr = ag.params.collisionQueryRange*0.25f;
			if (vDist2DSqr(ag.npos, ag.boundary.getCenter()) > sqr(updateThr) ||
				!ag.boundary.isValid(m_navquery, m_filters[ag.params.queryFilterType]))
			{
				ag.boundary.update(ag.corridor.getFirstPoly(), ag.npos, ag.params.collisionQueryRange,
									m_navquery, m_filters[ag.params.queryFilterType]);
			}
			// Query neighbour agents
			ag.neis = getNeighbours(ag.npos, ag.params.height, ag.params.collisionQueryRange,
									  ag, agents, m_grid);
		}
		
		// Find next corner to steer to.
		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE || ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;
			
			// Find corners for steering
			ag.corners = ag.corridor.findCorners(DT_CROWDAGENT_MAX_CORNERS, m_navquery, m_filters[ag.params.queryFilterType]);
			
			// Check to see if the corner after the next corner is directly visible,
			// and short cut to there.
			if ((ag.params.updateFlags & CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS) != 0 && ag.corners.size() > 0)
			{
				float[] target = ag.corners.get(Math.min(1,ag.corners.size()-1)).getPos();
				ag.corridor.optimizePathVisibility(target, ag.params.pathOptimizationRange, m_navquery, m_filters[ag.params.queryFilterType]);
				
				// Copy data for debug purposes.
				if (debugIdx == i)
				{
					vCopy(debug.optStart, ag.corridor.getPos());
					vCopy(debug.optEnd, target);
				}
			}
			else
			{
				// Copy data for debug purposes.
				if (debugIdx == i)
				{
					vSet(debug.optStart, 0,0,0);
					vSet(debug.optEnd, 0,0,0);
				}
			}
		}

		// Trigger off-mesh connections (depends on corners).
		for (CrowdAgent ag : agents)
		{
			
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE || ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
				continue;
			
			// Check 
			float triggerRadius = ag.params.radius*2.25f;
			if (ag.overOffmeshConnection(triggerRadius))
			{
				// Prepare to off-mesh connection.
				CrowdAgentAnimation anim = ag.animation;
				
				// Adjust the path over the off-mesh connection.
				long[] refs = new long[2];
				if (ag.corridor.moveOverOffmeshConnection(ag.corners.get(ag.corners.size() - 1).getRef(), refs,
														   anim.startPos, anim.endPos, m_navquery))
				{
					vCopy(anim.initPos, ag.npos);
					anim.polyRef = refs[1];
					anim.active = true;
					anim.t = 0.0f;
					anim.tmax = (vDist2D(anim.startPos, anim.endPos) / ag.params.maxSpeed) * 0.5f;
					
					ag.state = CrowdAgentState.DT_CROWDAGENT_STATE_OFFMESH;
					ag.corners.clear();
					ag.neis.clear();
					continue;
				}
				else
				{
					// Path validity check will ensure that bad/blocked connections will be replanned.
				}
			}
		}

		// Calculate steering.
		for (CrowdAgent ag : agents)
		{

			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE)
				continue;
			
			float[] dvel = new float[3];

			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
			{
				vCopy(dvel, ag.targetPos);
				ag.desiredSpeed = vLen(ag.targetPos);
			}
			else
			{
				// Calculate steering direction.
				if ((ag.params.updateFlags & CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS) != 0)
					dvel = ag.calcSmoothSteerDirection();
				else
					dvel = ag.calcStraightSteerDirection();
				// Calculate speed scale, which tells the agent to slowdown at the end of the path.
				float slowDownRadius = ag.params.radius*2;	// TODO: make less hacky.
				float speedScale = ag.getDistanceToGoal(slowDownRadius) / slowDownRadius;
					
				ag.desiredSpeed = ag.params.maxSpeed;
				dvel = vScale(dvel, ag.desiredSpeed * speedScale);
			}

			// Separation
			if ((ag.params.updateFlags & CrowdAgentParams.DT_CROWD_SEPARATION) != 0)
			{
				float separationDist = ag.params.collisionQueryRange; 
				float invSeparationDist = 1.0f / separationDist; 
				float separationWeight = ag.params.separationWeight;
				
				float w = 0;
				float[] disp = new float[3];
				
				for (int j = 0; j < ag.neis.size(); ++j)
				{
					CrowdAgent nei = agents.get(ag.neis.get(j).idx);
					
					float[] diff = vSub(ag.npos, nei.npos);
					diff[1] = 0;
					
					float distSqr = vLenSqr(diff);
					if (distSqr < 0.00001f)
						continue;
					if (distSqr > sqr(separationDist))
						continue;
					float dist = (float) Math.sqrt(distSqr);
					float weight = separationWeight * (1.0f - sqr(dist*invSeparationDist));
					
					disp = vMad(disp, diff, weight/dist);
					w += 1.0f;
				}
				
				if (w > 0.0001f)
				{
					// Adjust desired velocity.
					dvel = vMad(dvel, disp, 1.0f/w);
					// Clamp desired velocity to desired speed.
					float speedSqr = vLenSqr(dvel);
					float desiredSqr = sqr(ag.desiredSpeed);
					if (speedSqr > desiredSqr)
						dvel = vScale(dvel, desiredSqr/speedSqr);
				}
			}
			
			// Set the desired velocity.
			vCopy(ag.dvel, dvel);
		}
		
		// Velocity planning.	
		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			
			if ((ag.params.updateFlags & CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE) != 0)
			{
				m_obstacleQuery.reset();
				
				// Add neighbours as obstacles.
				for (int j = 0; j < ag.neis.size(); ++j)
				{
					CrowdAgent nei = agents.get(ag.neis.get(j).idx);
					m_obstacleQuery.addCircle(nei.npos, nei.params.radius, nei.vel, nei.dvel);
				}

				// Append neighbour segments as obstacles.
				for (int j = 0; j < ag.boundary.getSegmentCount(); ++j)
				{
					float[] s = ag.boundary.getSegment(j);
					float[] s3 = Arrays.copyOfRange(s, 3, 6);
					if (triArea2D(ag.npos, s, s3) < 0.0f)
						continue;
					m_obstacleQuery.addSegment(s, s3);
				}

				ObstacleAvoidanceDebugData vod = null;
				if (debugIdx == i) 
					vod = debug.vod;
				
				// Sample new safe velocity.
				boolean adaptive = true;
				int ns = 0;

				ObstacleAvoidanceParams params = m_obstacleQueryParams[ag.params.obstacleAvoidanceType];
					
				if (adaptive)
				{
					Tupple2<Integer, float[]> nsnvel = m_obstacleQuery.sampleVelocityAdaptive(ag.npos, ag.params.radius, ag.desiredSpeed,
																 ag.vel, ag.dvel, params, vod);
					ns = nsnvel.first;
					ag.nvel = nsnvel.second;
				}
				else
				{
					Tupple2<Integer, float[]> nsnvel = m_obstacleQuery.sampleVelocityGrid(ag.npos, ag.params.radius, ag.desiredSpeed,
															 ag.vel, ag.dvel, params, vod);
					ns = nsnvel.first;
					ag.nvel = nsnvel.second;
				}
				m_velocitySampleCount += ns;
			}
			else
			{
				// If not using velocity planning, new velocity is directly the desired velocity.
				vCopy(ag.nvel, ag.dvel);
			}
		}

		// Integrate.
		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			ag.integrate(dt);
		}
		
		// Handle collisions.
		
		for (int iter = 0; iter < 4; ++iter)
		{
			for (int i = 0; i < agents.size(); ++i)
			{
				CrowdAgent ag = agents.get(i);
				int idx0 = ag.getAgentIndex();
				if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
					continue;

				vSet(ag.disp, 0,0,0);
				
				float w = 0;

				for (int j = 0; j < ag.neis.size(); ++j)
				{
					CrowdAgent nei = agents.get(ag.neis.get(j).idx);
					int idx1 = nei.getAgentIndex();
					float[] diff = vSub(ag.npos, nei.npos);
					diff[1] = 0;
					
					float dist = vLenSqr(diff);
					if (dist > sqr(ag.params.radius + nei.params.radius))
						continue;
					dist = (float) Math.sqrt(dist);
					float pen = (ag.params.radius + nei.params.radius) - dist;
					if (dist < 0.0001f)
					{
						// Agents on top of each other, try to choose diverging separation directions.
						if (idx0 > idx1)
							vSet(diff, -ag.dvel[2],0,ag.dvel[0]);
						else
							vSet(diff, ag.dvel[2],0,-ag.dvel[0]);
						pen = 0.01f;
					}
					else
					{
						pen = (1.0f/dist) * (pen*0.5f) * COLLISION_RESOLVE_FACTOR;
					}
					
					ag.disp  = vMad(ag.disp, diff, pen);			
					
					w += 1.0f;
				}
				
				if (w > 0.0001f)
				{
					float iw = 1.0f / w;
					ag.disp = vScale(ag.disp, iw);
				}
			}
			
			for (int i = 0; i < agents.size(); ++i)
			{
				CrowdAgent ag = agents.get(i);
				if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
					continue;
				
				ag.npos = vAdd(ag.npos, ag.disp);
			}
		}

		for (int i = 0; i < agents.size(); ++i)
		{
			CrowdAgent ag = agents.get(i);
			if (ag.state != CrowdAgentState.DT_CROWDAGENT_STATE_WALKING)
				continue;
			
			// Move along navmesh.
			ag.corridor.movePosition(ag.npos, m_navquery, m_filters[ag.params.queryFilterType]);
			// Get valid constrained position back.
			vCopy(ag.npos, ag.corridor.getPos());

			// If not using path, truncate the corridor to just one poly.
			if (ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_NONE || ag.targetState == MoveRequestState.DT_CROWDAGENT_TARGET_VELOCITY)
			{
				ag.corridor.reset(ag.corridor.getFirstPoly(), ag.npos);
				ag.partial = false;
			}

		}
		
		// Update agents using off-mesh connection.
		for (int i = 0; i < m_maxAgents; ++i)
		{
			CrowdAgentAnimation anim = m_agents[i].animation;
			if (!anim.active)
				continue;
			CrowdAgent ag = m_agents[i];

			anim.t += dt;
			if (anim.t > anim.tmax)
			{
				// Reset animation
				anim.active = false;
				// Prepare agent for walking.
				ag.state = CrowdAgentState.DT_CROWDAGENT_STATE_WALKING;
				continue;
			}
			
			// Update position
			float ta = anim.tmax*0.15f;
			float tb = anim.tmax;
			if (anim.t < ta)
			{
				float u = tween(anim.t, 0.0f, ta);
				ag.npos = vLerp(anim.initPos, anim.startPos, u);
			}
			else
			{
				float u = tween(anim.t, ta, tb);
				ag.npos = vLerp(anim.startPos, anim.endPos, u);
			}
				
			// Update velocity.
			vSet(ag.vel, 0,0,0);
			vSet(ag.dvel, 0,0,0);
		}
	}

	public float[] getQueryExtents() {
		return m_ext;
	}

	public IQueryFilter getFilter(int i) {
		return i >=0 && i < DT_CROWD_MAX_QUERY_FILTER_TYPE ? m_filters[i] : null;
	}

}

