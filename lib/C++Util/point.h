/*
 * point.h
 *
 *  Created on: Mar 30, 2014
 *      Author: bst
 */

#pragma once

#include <ostream>
#include <vector>
#include <math.h>

class Point {
    float x,y;
 public:
    Point() {x=0;y=0;}
    Point(float _x, float _y) { x=_x; y=_y; };
    float X() const { return x; }
    float Y() const { return y; }
    void setX(float _x) { x=_x; }
    void setY(float _y) { y=_y; }
    float getRange() const { return sqrt(X()*X()+Y()*Y()); }
    // Theta is angle from [0,1] CCW
    float getTheta() const {
	float th=atan2(Y(),X())-M_PI/2;
	if (th<-M_PI)
	    th+=2*M_PI;
	return th;
    }
    void setThetaRange(float theta, float range) {
	setX(cos(theta+M_PI/2)*range); 
	setY(sin(theta+M_PI/2)*range);
    }
    Point rot90() const {
	return Point(-Y(),X()); 
    }
    Point flipY() const {
	return Point(X(),-Y());
    }
    Point flipX() const {
	return Point(-X(),Y());
    }
    // Rotate a point CCW arond origin  (x,0)-> (x-,0+) with +ve rotation
    Point rotate(float radians) const { 
	return Point(X()*cos(radians)-Y()*sin(radians),X()*sin(radians)+Y()*cos(radians));
    }
    Point rotateDegrees(float degrees) const { 
	return rotate(degrees*M_PI/180);
    }
    Point operator-(const Point &p2) const { return Point(X()-p2.X(),Y()-p2.Y()); }
    Point operator+(const Point &p2) const { return Point(X()+p2.X(),Y()+p2.Y()); }
    Point operator/(float s) const { return Point(X()/s,Y()/s); }
    Point operator*(float s) const { return Point(X()*s,Y()*s); }
    friend Point operator*(float s,Point p);
    Point operator-(float s) const { return Point(X()-s,Y()-s); }
    Point operator-() const { return Point(-X(),-Y()); }
    Point operator+(float s) const { return Point(X()+s,Y()+s); }
    float norm() const { return getRange(); }
    float dot(const Point &p2) const { return p2.X()*X()+p2.Y()*Y(); }
    friend std::ostream& operator<<(std::ostream &s, const Point &p);
    friend std::istream& operator>>(std::istream &s,  Point &p);
    Point min(const Point &p2) const { return Point(std::min(X(),p2.X()),std::min(Y(),p2.Y())); }
    Point max(const Point &p2) const { return Point(std::max(X(),p2.X()),std::max(Y(),p2.Y())); }
    bool operator==(const Point &p2) const { return X()==p2.X() && Y()==p2.Y(); }
    bool isNan() const { return isnan(x) || isnan(y); }
};

inline Point operator*(float s,Point p) { return Point(p.x*s,p.y*s); }

// TODO Don't really belong here
inline std::ostream& operator<< (std::ostream& os, const std::vector<int>& v) 
{
    os << "[";
    for (std::vector<int>::const_iterator ii = v.begin(); ii != v.end(); ++ii) {
        os << " " << *ii;
    }
    os << " ]";
    return os;
}

inline std::ostream& operator<< (std::ostream& os, const std::vector<float>& v) 
{
    os << "[";
    for (std::vector<float>::const_iterator ii = v.begin(); ii != v.end(); ++ii) {
        os << " " << *ii;
    }
    os << " ]";
    return os;
}

inline std::ostream& operator<< (std::ostream& os, const std::vector<Point>& v) 
{
    bool first=true;
    os << "[";
    for (std::vector<Point>::const_iterator ii = v.begin(); ii != v.end(); ++ii) {
	if (!first)
	    os << "; ";
        os << ii->X() << "," << ii->Y();
	first=false;
    }
    os << " ]";
    return os;
}

// Calculate distance from a line segment define by two points and another point
inline float  segment2pt(const Point &l1, const Point &l2, const Point &p) {
    Point D=l2-l1;
    Point p1=p-l1;
    float u=D.dot(p1)/D.dot(D);
    if (u<=0)
	return p1.norm();
    else if (u>=1)
	return (p-l2).norm();
    else 
	return (p1-D*u).norm();
}

