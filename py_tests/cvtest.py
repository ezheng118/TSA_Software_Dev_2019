import numpy as np
import cv2
import math

#rotates the image w/o cutting off parts of it
def rotate_bound(image, angle):
    # grab the dimensions of the image and then determine the
    # center
    (h, w) = image.shape[:2]
    (cX, cY) = (w // 2, h // 2)
 
    # grab the rotation matrix (applying the negative of the
    # angle to rotate clockwise), then grab the sine and cosine
    # (i.e., the rotation components of the matrix)
    M = cv2.getRotationMatrix2D((cX, cY), -angle, 1.0)
    cos = np.abs(M[0, 0])
    sin = np.abs(M[0, 1])
 
    # compute the new bounding dimensions of the image
    nW = int((h * sin) + (w * cos))
    nH = int((h * cos) + (w * sin))
 
    # adjust the rotation matrix to take into account translation
    M[0, 2] += (nW / 2) - cX
    M[1, 2] += (nH / 2) - cY
 
    # perform the actual rotation and return the image
    return cv2.warpAffine(image, M, (nW, nH))

def remove_outliers(contour_array):
    new_contour_array = []
    if len(contour_array) > 0:
        for contour in contour_array:
            if cv2.contourArea(contour) > 500:
                new_contour_array.append(contour)
    print(len(new_contour_array))
    return new_contour_array

def largest_contour(contour_array):
    largest = 0
    if len(contour_array) > 0:
        largest = contour_array[0]
        for contour in contour_array:
            print(str(cv2.contourArea(contour)) + ', ' + str(cv2.contourArea(largest)))
            if cv2.contourArea(contour) > cv2.contourArea(largest):
                largest = contour
    return largest

def resize_to_letter(img):
    resized_im = img
    #the document should have a width:height ratio of 8.5:11
    if(img.shape[0] != .773 * img.shape[1]):
        resized_im = cv2.resize(img, (int(img.shape[1] * 1.294), int(img.shape[0])))
    return resized_im

def top_down_transform(im, cont):
    #im is rotated 90 deg, so y is x axis, and x is y axis
    top_down = im

    """I have no idea how this finds the corners but it does lol"""
    perimeter = cv2.arcLength(cont, True)
    cont_approx = cv2.approxPolyDP(cont, 0.03*perimeter, True)

    #page is an array made from a simplified version of the contour passed to the function
    page = cont_approx[:, 0]

    #Sort corners: top left, top right, bot left, bot right
    diff = np.diff(page, axis=1)
    summ = page.sum(axis=1)
    pts = np.array([page[np.argmin(summ)], page[np.argmin(diff)], page[np.argmax(diff)], page[np.argmax(summ)]], dtype = "float32")

    #pts = [t_l, t_r, b_l, b_r]
    print(pts)
    
    """transform image to top down viewpoint"""
    #compute the width of the new image, which will be the maximum distance between 
    #the bottom-right and bottom-left x-coordiates or the top-right and top-left x-coordinates
    widthA = np.sqrt(((pts[3][0] - pts[2][0]) ** 2) + ((pts[3][1] - pts[2][1]) ** 2))
    widthB = np.sqrt(((pts[1][0] - pts[0][0]) ** 2) + ((pts[1][1] - pts[0][1]) ** 2))
    maxWidth = max(int(widthA), int(widthB))

    #compute the height of the new image, which will be the maximum distance between
    #the top-right and bottom-right y-coordinates or the top-left and bottom-left y-coordinates
    heightA = np.sqrt(((pts[1][0] - pts[3][0]) ** 2) + ((pts[1][1] - pts[3][1]) ** 2))
    heightB = np.sqrt(((pts[0][0] - pts[2][0]) ** 2) + ((pts[0][1] - pts[2][1]) ** 2))
    maxHeight = max(int(heightA), int(heightB))
    
    #dst = np.zeros_like(pts), pts and dst should have the same shape
    #dst[0] = top left = [0, 0]
    #dst[1] = top right = [maxWidth - 1, 0]
    #dst[2] = bottom left = [0, maxHeight - 1]
    #dst[3] = bottom right = [maxWidth - 1, maxHeight - 1]
    dst = np.array([[0, 0], [maxWidth - 1, 0], [0, maxHeight - 1], [maxWidth - 1, maxHeight - 1]], dtype = "float32")
    print(dst)

    mat = cv2.getPerspectiveTransform(pts, dst)
    top_down = cv2.warpPerspective(im, mat, (maxWidth, maxHeight))

    return top_down, pts

def find_doc_contours(image):
    img = cv2.imread(image)
    #photo was taken in protrait, and is too tall to display so it is rotated
    img = rotate_bound(img, 90)

    #still too large so the image is resized to see what is happening
    #im_width = img.shape[0] / 4
    #im_height = img.shape[1] / 4
    #small_img = cv2.resize(img, (int(im_height), int(im_width)))
    small_img = img

    #convert image to grayscale to perform thresholding
    HSV_im = cv2.cvtColor(small_img, cv2.COLOR_BGR2HSV)

    #H values for white go from H < 50, H > 130
    #need to take two thresholded image to get all the white colors
    lower_white1 = np.array([0, 0, 140])
    upper_white1 = np.array([50, 50, 255])
    lower_white2 = np.array([90, 0, 140])
    upper_white2 = np.array([180, 50, 255])

    #simple thresholding on the image to separate the document from the background
    thresh1 = cv2.inRange(HSV_im, lower_white1, upper_white1)
    thresh2 = cv2.inRange(HSV_im, lower_white2, upper_white2)
    
    #use a bitwise or to combine the two images into one
    combined_thresh = cv2.bitwise_or(thresh1, thresh2)

    #remove any noise in the image, try and find the outline of the document
    kernel = np.ones((10, 10), np.uint8)
    mask = cv2.morphologyEx(combined_thresh, cv2.MORPH_CLOSE, kernel)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)

    edges = cv2.Canny(mask, 100, 200)
    contours, hierarchy = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)

    #contours = remove_outliers(contours)
    #assumes that the largest contour is the document's outline
    document_contour = largest_contour(contours)
    cv2.drawContours(small_img, contours, -1, (0, 255, 0), 15)

    docview, corners = top_down_transform(small_img, document_contour)

    for pt in corners:
        pt_tup = (pt[0], pt[1])
        cv2.circle(small_img, pt_tup, 18, (255, 0, 255), -1)
    
    docview = resize_to_letter(docview)
    docview = rotate_bound(docview, -90)

    cv2.imshow(image + ':test', small_img)
    #cv2.imshow(image + ':HSV', HSV_im)
    #cv2.imshow(image + ':threshold', thresh1)
    #cv2.imshow(image + ':threshold2', thresh2)
    #cv2.imshow(image + ':combined', combined_thresh)
    #cv2.imshow(image + ':mask', mask)
    cv2.imshow('doc view', docview)
    #cv2.imshow(image + ':edges', edges)

    key = cv2.waitKey(0)

    #print(img.shape)
    #print(small_img.shape)
    #print(contours)

    cv2.destroyAllWindows()

    cv2.imwrite("scanned_doc.jpg", docview)
    cv2.imwrite("imgAndContour.jpg", small_img)
    #cv2.imwrite("HSV.jpg", HSV_im)
    #cv2.imwrite("thresh1.jpg", thresh1)
    #cv2.imwrite("thresh2.jpg", thresh2)
    #cv2.imwrite("combined.jpg", combined_thresh)
    #cv2.imwrite("mask.jpg", mask)
    #cv2.imwrite("cannyEdges.jpg", edges)

if __name__ == "__main__":
    #image = "test_imgs/005.jpg"
    image = "test_imgs/003.jpg"
    find_doc_contours(image)