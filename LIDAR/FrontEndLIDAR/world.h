/*
 * world.h
 *
 *  Created on: Mar 25, 2014
 *      Author: bst
 */

#ifndef WORLD_H_
#define WORLD_H_

#include "classifier.h"
#include "sickio.h"
#include "person.h"

class Vis;

class World {
    std::vector<Person> people;
public:
    World();
    // Track people and send update messages
    void track(const Vis *vis);
    mxArray *convertToMX() const;
};

#endif  /* WORLD_H_ */