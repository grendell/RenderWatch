#pragma version(1)
#pragma rs java_package_name(com.grendell.renderwatch)

int2 gCenter;
int2 gHourHandEndpoint;
int gHourHandWidthSqr;
int2 gMinuteHandEndpoint;
int gMinuteHandWidthSqr;
int2 gSecondHandEndpoint;
int gSecondHandWidthSqr;

void init() {
}

static float getDistSqr(int2 v, int2 w, uint32_t x, uint32_t y) {
    int lenSqr = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y);
    
    if (lenSqr == 0) {
        return (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
    }
    
    int dot = (x - v.x) * (w.x - v.x) + (y - v.y) * (w.y - v.y);
    float t = (float)dot / (float)lenSqr;
    
    if (t < 0.0f) {
        return (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
    } else if (t > 1.0f) {
        return (w.x - x) * (w.x - x) + (w.y - y) * (w.y - y);
    } else {
        float px = v.x + t * (w.x - v.x);
        float py = v.y + t * (w.y - v.y);
        return (px - x) * (px - x) + (py - y) * (py - y);
    }
}

uint32_t __attribute__((kernel)) render(uint32_t in, uint32_t x, uint32_t y) {
    float hourDistSqr = getDistSqr(gCenter, gHourHandEndpoint, x, y);
    float minuteDistSqr = getDistSqr(gCenter, gMinuteHandEndpoint, x, y);
    float secondDistSqr = getDistSqr(gCenter, gSecondHandEndpoint, x, y);

    uchar a = 0xff;
    uchar r = (uchar)(0xff * clamp((gHourHandWidthSqr - hourDistSqr) / gHourHandWidthSqr, 0.0f, 1.0f));
    uchar g = (uchar)(0xff * clamp((gMinuteHandWidthSqr - minuteDistSqr) / gMinuteHandWidthSqr, 0.0f, 1.0f));
    uchar b = (uchar)(0xff * clamp((gSecondHandWidthSqr - secondDistSqr) / gSecondHandWidthSqr, 0.0f, 1.0f));

    return (a << 24) | (r << 16) | (g << 8) | (b);
}