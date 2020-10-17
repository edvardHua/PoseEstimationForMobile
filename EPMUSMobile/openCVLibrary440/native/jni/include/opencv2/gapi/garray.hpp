// This file is part of OpenCV project.
// It is subject to the license terms in the LICENSE file found in the top-level directory
// of this distribution and at http://opencv.org/license.html.
//
// Copyright (C) 2018 Intel Corporation


#ifndef OPENCV_GAPI_GARRAY_HPP
#define OPENCV_GAPI_GARRAY_HPP

#include <functional>
#include <ostream>
#include <vector>
#include <memory>

#include <opencv2/gapi/own/exports.hpp>
#include <opencv2/gapi/opencv_includes.hpp>

#include <opencv2/gapi/util/variant.hpp>
#include <opencv2/gapi/util/throw.hpp>
#include <opencv2/gapi/own/assert.hpp>

#include <opencv2/gapi/gmat.hpp>    // flatten_g only!
#include <opencv2/gapi/gscalar.hpp> // flatten_g only!

namespace cv
{
// Forward declaration; GNode and GOrigin are an internal
// (user-inaccessible) classes.
class GNode;
struct GOrigin;
template<typename T> class GArray;

/**
 * \addtogroup gapi_meta_args
 * @{
 */
struct GArrayDesc
{
    // FIXME: Body
    // FIXME: Also implement proper operator== then
    bool operator== (const GArrayDesc&) const { return true; }
};
template<typename U> GArrayDesc descr_of(const std::vector<U> &) { return {};}
static inline GArrayDesc empty_array_desc() {return {}; }
/** @} */

std::ostream& operator<<(std::ostream& os, const cv::GArrayDesc &desc);

namespace detail
{
    // FIXME: This type spec needs to be:
    // 1) shared with GOpaque (not needed right now)
    // 2) unified with the serialization (S11N, not merged right now).
    // Adding it to type traits is problematic due to our header deps
    // (which also need to be fixed).
    enum class TypeSpec: int {
        OPAQUE_SPEC,
        MAT,
        RECT
    };
    // FIXME: Reuse the below from "opaque traits" of S11N!
    template<typename T> struct GTypeSpec;
    template<typename T> struct GTypeSpec
    {
        static constexpr const TypeSpec spec = TypeSpec::OPAQUE_SPEC;
    };
    template<>           struct GTypeSpec<cv::Mat>
    {
        static constexpr const TypeSpec spec = TypeSpec::MAT;
    };
    template<>           struct GTypeSpec<cv::Rect>
    {
        static constexpr const TypeSpec spec = TypeSpec::RECT;
    };

    // ConstructVec is a callback which stores information about T and is used by
    // G-API runtime to construct arrays in host memory (T remains opaque for G-API).
    // ConstructVec is carried into G-API internals by GArrayU.
    // Currently it is suitable for Host (CPU) plugins only, real offload may require
    // more information for manual memory allocation on-device.
    class VectorRef;
    using ConstructVec = std::function<void(VectorRef&)>;

    // This is the base struct for GArrayU type holder
    struct TypeHintBase{virtual ~TypeHintBase() = default;};

    // This class holds type of initial GArray to be checked from GArrayU
    template <typename T>
    struct TypeHint final : public TypeHintBase{};

    // This class strips type information from GArray<T> and makes it usable
    // in the G-API graph compiler (expression unrolling, graph generation, etc).
    // Part of GProtoArg.
    class GAPI_EXPORTS GArrayU
    {
    public:
        GArrayU(const GNode &n, std::size_t out); // Operation result constructor

        template <typename T>
        bool holds() const;                       // Check if was created from GArray<T>

        GOrigin& priv();                          // Internal use only
        const GOrigin& priv() const;              // Internal use only

    protected:
        GArrayU();                                // Default constructor
        template<class> friend class cv::GArray;  //  (available to GArray<T> only)

        void setConstructFcn(ConstructVec &&cv);  // Store T-aware constructor

        template <typename T>
        void specifyType();                       // Store type of initial GArray<T>

        std::shared_ptr<GOrigin> m_priv;
        std::shared_ptr<TypeHintBase> m_hint;
    };

    template <typename T>
    bool GArrayU::holds() const{
        GAPI_Assert(m_hint != nullptr);
        using U = typename std::decay<T>::type;
        return dynamic_cast<TypeHint<U>*>(m_hint.get()) != nullptr;
    };

    template <typename T>
    void GArrayU::specifyType(){
        m_hint.reset(new TypeHint<typename std::decay<T>::type>);
    };

    // This class represents a typed STL vector reference.
    // Depending on origins, this reference may be either "just a" reference to
    // an object created externally, OR actually own the underlying object
    // (be value holder).
    class BasicVectorRef
    {
    public:
        // These fields are set by the derived class(es)
        std::size_t    m_elemSize = 0ul;
        cv::GArrayDesc m_desc;
        TypeSpec       m_spec;
        virtual ~BasicVectorRef() {}

        virtual void mov(BasicVectorRef &ref) = 0;
        virtual const void* ptr() const = 0;
        virtual std::size_t size() const = 0;
    };

    template<typename T> class VectorRefT final: public BasicVectorRef
    {
        using empty_t  = util::monostate;
        using ro_ext_t = const std::vector<T> *;
        using rw_ext_t =       std::vector<T> *;
        using rw_own_t =       std::vector<T>  ;
        util::variant<empty_t, ro_ext_t, rw_ext_t, rw_own_t> m_ref;

        inline bool isEmpty() const { return util::holds_alternative<empty_t>(m_ref);  }
        inline bool isROExt() const { return util::holds_alternative<ro_ext_t>(m_ref); }
        inline bool isRWExt() const { return util::holds_alternative<rw_ext_t>(m_ref); }
        inline bool isRWOwn() const { return util::holds_alternative<rw_own_t>(m_ref); }

        void init(const std::vector<T>* vec = nullptr)
        {
            m_elemSize = sizeof(T);
            if (vec) m_desc = cv::descr_of(*vec);
            m_spec = GTypeSpec<T>::spec;
        }

    public:
        VectorRefT() { init(); }
        virtual ~VectorRefT() {}

        explicit VectorRefT(const std::vector<T>& vec) : m_ref(&vec)      { init(&vec); }
        explicit VectorRefT(std::vector<T>& vec)  : m_ref(&vec)           { init(&vec); }
        explicit VectorRefT(std::vector<T>&& vec) : m_ref(std::move(vec)) { init(&vec); }

        // Reset a VectorRefT. Called only for objects instantiated
        // internally in G-API (e.g. temporary GArray<T>'s within a
        // computation).  Reset here means both initialization
        // (creating an object) and reset (discarding its existing
        // content before the next execution).  Must never be called
        // for external VectorRefTs.
        void reset()
        {
            if (isEmpty())
            {
                std::vector<T> empty_vector;
                m_desc = cv::descr_of(empty_vector);
                m_ref  = std::move(empty_vector);
                GAPI_Assert(isRWOwn());
            }
            else if (isRWOwn())
            {
                util::get<rw_own_t>(m_ref).clear();
            }
            else GAPI_Assert(false); // shouldn't be called in *EXT modes
        }

        // Obtain a WRITE reference to underlying object
        // Used by CPU kernel API wrappers when a kernel execution frame
        // is created
        std::vector<T>& wref()
        {
            GAPI_Assert(isRWExt() || isRWOwn());
            if (isRWExt()) return *util::get<rw_ext_t>(m_ref);
            if (isRWOwn()) return  util::get<rw_own_t>(m_ref);
            util::throw_error(std::logic_error("Impossible happened"));
        }

        // Obtain a READ reference to underlying object
        // Used by CPU kernel API wrappers when a kernel execution frame
        // is created
        const std::vector<T>& rref() const
        {
            // ANY vector can be accessed for reading, even if it declared for
            // output. Example -- a GComputation from [in] to [out1,out2]
            // where [out2] is a result of operation applied to [out1]:
            //
            //            GComputation boundary
            //            . . . . . . .
            //            .           .
            //     [in] ----> foo() ----> [out1]
            //            .           .    :
            //            .           . . .:. . .
            //            .                V    .
            //            .              bar() ---> [out2]
            //            . . . . . . . . . . . .
            //
            if (isROExt()) return *util::get<ro_ext_t>(m_ref);
            if (isRWExt()) return *util::get<rw_ext_t>(m_ref);
            if (isRWOwn()) return  util::get<rw_own_t>(m_ref);
            util::throw_error(std::logic_error("Impossible happened"));
        }

        virtual void mov(BasicVectorRef &v) override {
            VectorRefT<T> *tv = dynamic_cast<VectorRefT<T>*>(&v);
            GAPI_Assert(tv != nullptr);
            wref() = std::move(tv->wref());
        }


        virtual const void* ptr() const override { return &rref(); }
        virtual std::size_t size() const override { return rref().size(); }
    };

    // This class strips type information from VectorRefT<> and makes it usable
    // in the G-API executables (carrying run-time data/information to kernels).
    // Part of GRunArg.
    // Its methods are typed proxies to VectorRefT<T>.
    // VectorRef maintains "reference" semantics so two copies of VectoRef refer
    // to the same underlying object.
    // FIXME: Put a good explanation on why cv::OutputArray doesn't fit this role
    class VectorRef
    {
        std::shared_ptr<BasicVectorRef> m_ref;

        template<typename T> inline void check() const
        {
            GAPI_DbgAssert(dynamic_cast<VectorRefT<T>*>(m_ref.get()) != nullptr);
            GAPI_Assert(sizeof(T) == m_ref->m_elemSize);
        }

    public:
        VectorRef() = default;
        template<typename T> explicit VectorRef(const std::vector<T>& vec) : m_ref(new VectorRefT<T>(vec)) {}
        template<typename T> explicit VectorRef(std::vector<T>& vec)       : m_ref(new VectorRefT<T>(vec)) {}
        template<typename T> explicit VectorRef(std::vector<T>&& vec)      : m_ref(new VectorRefT<T>(vec)) {}

        template<typename T> void reset()
        {
            if (!m_ref) m_ref.reset(new VectorRefT<T>());

            check<T>();
            static_cast<VectorRefT<T>&>(*m_ref).reset();
        }

        template<typename T> std::vector<T>& wref()
        {
            check<T>();
            return static_cast<VectorRefT<T>&>(*m_ref).wref();
        }

        template<typename T> const std::vector<T>& rref() const
        {
            check<T>();
            return static_cast<VectorRefT<T>&>(*m_ref).rref();
        }

        void mov(VectorRef &v)
        {
            m_ref->mov(*v.m_ref);
        }

        cv::GArrayDesc descr_of() const
        {
            return m_ref->m_desc;
        }

        std::size_t size() const
        {
            return m_ref->size();
        }

        // May be used to uniquely identify this object internally
        const void *ptr() const { return m_ref->ptr(); }

        TypeSpec spec() const
        {
            return m_ref->m_spec;
        }
    };

    // Helper (FIXME: work-around?)
    // stripping G types to their host types
    // like cv::GArray<GMat> would still map to std::vector<cv::Mat>
    // but not to std::vector<cv::GMat>
#if defined(GAPI_STANDALONE)
#  define FLATTEN_NS cv::gapi::own
#else
#  define FLATTEN_NS cv
#endif
    template<class T> struct flatten_g;
    template<> struct flatten_g<cv::GMat>    { using type = FLATTEN_NS::Mat; };
    template<> struct flatten_g<cv::GScalar> { using type = FLATTEN_NS::Scalar; };
    template<class T> struct flatten_g       { using type = T; };
#undef FLATTEN_NS
    // FIXME: the above mainly duplicates "ProtoToParam" thing from gtyped.hpp
    // but I decided not to include gtyped here - probably worth moving that stuff
    // to some common place? (DM)
} // namespace detail

/** \addtogroup gapi_data_objects
 * @{
 */

template<typename T> class GArray
{
public:
    GArray() { putDetails(); }             // Empty constructor
    explicit GArray(detail::GArrayU &&ref) // GArrayU-based constructor
        : m_ref(ref) { putDetails(); }     //   (used by GCall, not for users)

    detail::GArrayU strip() const { return m_ref; }

private:
    // Host type (or Flat type) - the type this GArray is actually
    // specified to.
    using HT = typename detail::flatten_g<typename std::decay<T>::type>::type;

    static void VCTor(detail::VectorRef& vref) {
        vref.reset<HT>();
    }
    void putDetails() {
        m_ref.setConstructFcn(&VCTor);
        m_ref.specifyType<HT>();
    }

    detail::GArrayU m_ref;
};

/** @} */

} // namespace cv

#endif // OPENCV_GAPI_GARRAY_HPP
