package com.kitaab.app.data.local.db

import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

fun UserProfile.toCached() =
    CachedUserProfile(
        id = id,
        name = name,
        email = email,
        city = city,
        pincode = pincode,
        locality = locality,
        profilePhotoUrl = profilePhotoUrl,
        avgRating = avgRating,
        reviewCount = reviewCount,
        badge = badge,
        examTags = examTags,
        totalSold = totalSold,
        totalDonated = totalDonated,
        strikeCount = strikeCount,
        isSuspended = isSuspended,
        createdAt = createdAt,
    )

fun CachedUserProfile.toDomain() =
    UserProfile(
        id = id,
        name = name,
        email = email,
        city = city,
        pincode = pincode,
        locality = locality,
        profilePhotoUrl = profilePhotoUrl,
        avgRating = avgRating,
        reviewCount = reviewCount,
        badge = badge,
        examTags = examTags,
        totalSold = totalSold,
        totalDonated = totalDonated,
        strikeCount = strikeCount,
        isSuspended = isSuspended,
        createdAt = createdAt,
    )

fun Listing.toCached() =
    CachedListing(
        id = id,
        sellerId = sellerId,
        title = title,
        author = author,
        publisher = publisher,
        edition = edition,
        isbn = isbn,
        subject = subject,
        examTags = examTags,
        condition = condition,
        type = type,
        price = price,
        photoUrls = photoUrls,
        hasSolutions = hasSolutions,
        hasNotes = hasNotes,
        status = status,
        city = city,
        pincode = pincode,
        locality = locality,
        isBundle = isBundle,
        createdAt = createdAt,
    )

fun CachedListing.toDomain() =
    Listing(
        id = id,
        sellerId = sellerId,
        title = title,
        author = author,
        publisher = publisher,
        edition = edition,
        isbn = isbn,
        subject = subject,
        examTags = examTags,
        condition = condition,
        type = type,
        price = price,
        photoUrls = photoUrls,
        hasSolutions = hasSolutions,
        hasNotes = hasNotes,
        status = status,
        city = city,
        pincode = pincode,
        locality = locality,
        isBundle = isBundle,
        createdAt = createdAt,
    )
